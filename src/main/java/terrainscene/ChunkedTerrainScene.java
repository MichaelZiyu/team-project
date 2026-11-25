package terrainscene;

import game.World;
import game.gameModel.Renderable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import jswing.WindowManager;
import terrain.OverworldTerrain;
import terrain.Terrain;
import terrain.TerrainCoordinates;
import terrain.Trees;

/**
 * The terrain around the player as a handful of pre-painted chunk images, prepared ahead of the
 * camera so walking stays smooth.
 *
 * <p>The scene keeps every chunk that is on screen, plus a border of {@code renderDistance}
 * chunks around it, painted and ready. As the camera moves, chunks entering that border are
 * painted on background threads and appear once ready; chunks well outside it are dropped to
 * save memory. Because the border is prepared in advance, the edge of the screen should reach a
 * chunk only after it has been painted.
 *
 * <p>Shadows and sunlight are painted into the chunk images, so when the sun moves far enough to
 * notice, every chunk is repainted in the background for the new lighting. Until all of them are
 * ready the old images stay on screen; then they are all swapped at once, so neighbouring chunks
 * never show the sun in two different places. A repaint is never abandoned for a newer sun: it
 * finishes first, and the next one starts from wherever the sun is by then, so lighting keeps
 * stepping forward even when time runs fast. Chunks are painted on several threads at once.
 *
 * <p>Each chunk's water is a separate image drawn underneath all the terrain, with a sheet of
 * moving ripples ({@link RippleSheet}) just above it.
 *
 * <p>Call {@link #update} and iterate on the Swing thread (the game's render step does both). The
 * renderer draws the first sprite on top, so chunks nearest the camera come first, and the water
 * after all of them.
 */
public final class ChunkedTerrainScene implements Iterable<Renderable> {

  /** Paints one chunk; lets tests replace painting with something simple. */
  @FunctionalInterface
  interface ChunkSource {

    /**
     * Paints a chunk. May be called on several background threads at once.
     *
     * @param chunkX the chunk column, in chunks
     * @param chunkY the chunk row, in chunks
     * @param sun how the sun lights the chunk
     * @return the painted chunk
     */
    ChunkImage paint(int chunkX, int chunkY, TerrainLight sun);
  }

  /** A chunk's position, in chunks. */
  private record ChunkKey(int chunkX, int chunkY) {}

  /**
   * A chunk to paint for one lighting. Each time the sun moves far enough to repaint, the
   * lighting gets a new generation number, so leftover work for older lighting can be recognised.
   */
  private record Request(ChunkKey chunk, int generation) {}

  /** A chunk painted for one lighting generation. */
  private record Painted(Request request, ChunkImage image) {}

  /** A chunk ready to draw: its terrain, and its water underneath ({@code null} if none). */
  private record Shown(ChunkRenderable terrain, ChunkRenderable water) {}

  /** How many chunks are prepared beyond each edge of the screen unless told otherwise. */
  public static final int DEFAULT_RENDER_DISTANCE = World.TERRAIN_RENDER_DISTANCE;

  /**
   * How far raised tiles can be lifted into view from below the screen, in pixels: 16 height
   * levels. Chunks that far below the screen are included so tall hills never pop in.
   */
  static final int MAX_LIFT_PX = 16 * TileRenderable.STEP_HEIGHT_PX;

  /**
   * How far, in pixels per level of height, shadows must move before the chunks are repainted.
   * With a 10-minute day this repaints about every 8 seconds on average (as often as every
   * second mid-morning, never at night), in small steps that are hard to notice.
   */
  static final double REPAINT_SHADOW_PX = 4;

  /**
   * How much the brightness of any rim (from 0 to 1) may change before the chunks are repainted,
   * so rims brighten and darken in small steps that read as gradual.
   */
  static final double REPAINT_RIM_LIGHT = 0.08;

  /**
   * How many threads paint chunks: half the processors, at least one and at most four, so the
   * game itself keeps enough processor time on smaller machines.
   */
  static final int PAINT_THREADS =
      Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));

  /** How much sunlight must strengthen or weaken before the chunks are repainted. */
  static final double REPAINT_STRENGTH = 0.15;

  private static final int CHUNK_PX =
      TerrainCoordinates.CHUNK_SIZE * TerrainCoordinates.BLOCK_SIZE_PX;

  private final ChunkSource source;
  private final Executor executor;
  private final int viewWidthPx;
  private final int viewHeightPx;
  private final int renderDistance;

  /** The moving ripples over the water; {@code null} in tests, which leave them out. */
  private final RippleSheet ripples;

  /** Chunks painted on background threads and waiting to be shown. */
  private final Queue<Painted> finished = new ConcurrentLinkedQueue<>();

  /** Chunks being painted right now, so they are not requested twice. */
  private final Set<Request> pending = ConcurrentHashMap.newKeySet();

  /** Chunks ready to draw; only touched on the Swing thread. */
  private final Map<ChunkKey, Shown> displayed = new HashMap<>();

  /** Chunks already painted for new lighting, waiting until all are ready to be swapped in. */
  private final Map<ChunkKey, Shown> staged = new HashMap<>();

  /** The lighting generation of the chunks on screen. */
  private int shownGeneration;

  /** The newest lighting generation; read by painting threads to skip outdated work. */
  private volatile int targetGeneration;

  /** The newest lighting, or {@code null} before the first update. */
  private TerrainLight targetSun;

  /**
   * The light {@link #update(double, double)} paints in; changed with {@link #setLight}, possibly
   * from another thread than the one updating.
   */
  private volatile TerrainLight light = TerrainLight.DEFAULT;

  private List<Renderable> drawOrder = List.of();

  /**
   * Creates a scene showing the overworld generated from a seed, sized for the game's window and
   * the game's render distance.
   *
   * @param seed the world seed; the same seed always shows the same world
   */
  public ChunkedTerrainScene(long seed) {
    this(
        seed,
        WindowManager.FRAME_WIDTH,
        WindowManager.FRAME_HEIGHT,
        World.TERRAIN_RENDER_DISTANCE);
  }

  /**
   * Creates a scene showing the overworld generated from a seed, painting its chunks on its own
   * background threads.
   *
   * <p>The scene makes its own terrain from the seed. If other code (such as collision) also
   * needs the terrain, make one with {@link OverworldTerrain#fromSeed} and pass it to {@link
   * #create} instead, so both share it.
   *
   * @param seed the world seed; the same seed always shows the same world
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   * @param renderDistance how many chunks to prepare beyond each screen edge; at least 0
   * @throws IllegalArgumentException if {@code renderDistance} is negative
   */
  public ChunkedTerrainScene(long seed, int viewWidthPx, int viewHeightPx, int renderDistance) {
    this(
        new ChunkPainter(OverworldTerrain.fromSeed(seed), new TerrainImages()),
        newPaintingThreads(),
        viewWidthPx,
        viewHeightPx,
        renderDistance,
        true);
  }

  /**
   * Creates a scene with a given way of painting and running work, and no moving ripples; used
   * by tests.
   *
   * @param source paints each chunk
   * @param executor runs painting work, normally on background threads
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   * @param renderDistance how many chunks to prepare beyond each screen edge; at least 0
   * @throws IllegalArgumentException if {@code renderDistance} is negative
   */
  ChunkedTerrainScene(
      ChunkSource source,
      Executor executor,
      int viewWidthPx,
      int viewHeightPx,
      int renderDistance) {
    this(source, executor, viewWidthPx, viewHeightPx, renderDistance, false);
  }

  /**
   * Creates a scene with a given way of painting and running work.
   *
   * @param source paints each chunk
   * @param executor runs painting work, normally on background threads
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   * @param renderDistance how many chunks to prepare beyond each screen edge; at least 0
   * @param withRipples whether to show moving ripples over the water
   * @throws IllegalArgumentException if {@code renderDistance} is negative
   */
  private ChunkedTerrainScene(
      ChunkSource source,
      Executor executor,
      int viewWidthPx,
      int viewHeightPx,
      int renderDistance,
      boolean withRipples) {
    if (renderDistance < 0) {
      throw new IllegalArgumentException("renderDistance must not be negative: " + renderDistance);
    }
    this.source = source;
    this.executor = executor;
    this.viewWidthPx = viewWidthPx;
    this.viewHeightPx = viewHeightPx;
    this.renderDistance = renderDistance;
    this.ripples = withRipples ? new RippleSheet(viewWidthPx, viewHeightPx) : null;
  }

  /**
   * Creates a scene that paints the given terrain's chunks on its own background threads.
   *
   * @param terrain the terrain to show
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   * @param renderDistance how many chunks to prepare beyond each screen edge; at least 0
   * @return the scene
   * @throws IllegalArgumentException if {@code renderDistance} is negative
   */
  public static ChunkedTerrainScene create(
      Terrain terrain, int viewWidthPx, int viewHeightPx, int renderDistance) {
    return create(terrain, null, viewWidthPx, viewHeightPx, renderDistance);
  }

  /**
   * Creates a scene that paints the given terrain's chunks on its own background threads, with
   * the shadows of the trees growing on it.
   *
   * @param terrain the terrain to show
   * @param trees the trees growing on it, whose shadows fall on it; {@code null} for none
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   * @param renderDistance how many chunks to prepare beyond each screen edge; at least 0
   * @return the scene
   * @throws IllegalArgumentException if {@code renderDistance} is negative
   */
  public static ChunkedTerrainScene create(
      Terrain terrain, Trees trees, int viewWidthPx, int viewHeightPx, int renderDistance) {
    ChunkSource painter = new ChunkPainter(terrain, trees, new TerrainImages());
    return new ChunkedTerrainScene(
        painter, newPaintingThreads(), viewWidthPx, viewHeightPx, renderDistance, true);
  }

  /** Returns {@link #PAINT_THREADS} daemon threads for painting chunks in the background. */
  private static Executor newPaintingThreads() {
    return Executors.newFixedThreadPool(
        PAINT_THREADS,
        task -> {
          Thread thread = new Thread(task, "terrain-chunks");
          thread.setDaemon(true);
          return thread;
        });
  }

  /**
   * Changes the light the terrain is painted in, such as dimmer light under rain cloud. The chunks
   * are repainted for it, in the background, during the next updates.
   *
   * @param light the new light
   */
  public void setLight(TerrainLight light) {
    this.light = light;
  }

  /**
   * Returns the light the terrain is painted in, or about to be.
   *
   * @return the light last set; {@link TerrainLight#DEFAULT} unless changed
   */
  public TerrainLight getLight() {
    return light;
  }

  /**
   * Brings the scene up to date for the player's position, in the light last set with {@link
   * #setLight}.
   *
   * @param playerX the player's x position, in pixels; the camera is centred on it
   * @param playerY the player's y position, in pixels
   * @see #update(double, double, TerrainLight)
   */
  public void update(double playerX, double playerY) {
    update(playerX, playerY, light);
  }

  /**
   * Brings the scene up to date: shows chunks that finished painting, drops chunks far off
   * screen, and starts painting chunks entering the border. If the lighting has changed enough to
   * notice since the chunks were painted, and no repaint is already under way, starts repainting
   * all of them for the new lighting.
   *
   * @param playerX the player's x position, in pixels; the camera is centred on it
   * @param playerY the player's y position, in pixels
   * @param sun how the sun lights the terrain now
   */
  public void update(double playerX, double playerY, TerrainLight sun) {
    if (ripples != null) {
      ripples.moveTo(playerX, playerY);
    }
    if (targetSun == null) {
      targetSun = sun;
    } else if (!isRelighting() && noticeablyDifferent(sun, targetSun)) {
      targetSun = sun;
      targetGeneration++;
      staged.clear();
    }
    List<ChunkKey> wanted = chunksAround(playerX, playerY, renderDistance);
    Set<ChunkKey> keep = new LinkedHashSet<>(chunksAround(playerX, playerY, renderDistance + 1));
    boolean changed = showFinished(keep);
    changed |= displayed.keySet().retainAll(keep);
    staged.keySet().retainAll(keep);
    TerrainLight lighting = targetSun;
    for (ChunkKey chunk : wanted) {
      boolean ready = isRelighting() ? staged.containsKey(chunk) : displayed.containsKey(chunk);
      Request request = new Request(chunk, targetGeneration);
      if (!ready && pending.add(request)) {
        executor.execute(() -> paintInBackground(request, lighting));
      }
    }
    changed |= showFinished(keep);
    if (isRelighting() && staged.keySet().containsAll(wanted)) {
      displayed.clear();
      displayed.putAll(staged);
      staged.clear();
      shownGeneration = targetGeneration;
      changed = true;
    }
    if (changed) {
      List<Shown> ordered = new ArrayList<>(displayed.values());
      ordered.sort(
          Comparator.comparingInt((Shown shown) -> shown.terrain().getChunkY())
              .thenComparingInt(shown -> shown.terrain().getChunkX()));
      List<Renderable> order = new ArrayList<>();
      for (Shown shown : ordered) {
        order.add(shown.terrain());
      }
      // The renderer draws the first sprite on top, so the water goes last, underneath it all,
      // with its ripples just above it.
      List<Renderable> water = new ArrayList<>();
      for (Shown shown : ordered) {
        if (shown.water() != null) {
          water.add(shown.water());
        }
      }
      if (ripples != null && !water.isEmpty()) {
        order.add(ripples);
      }
      order.addAll(water);
      drawOrder = Collections.unmodifiableList(order);
    }
  }

  /**
   * Returns how many chunks are ready to draw.
   *
   * @return the number of chunk images currently shown
   */
  public int displayedCount() {
    return displayed.size();
  }

  /**
   * Returns how many chunks are still being painted.
   *
   * @return the number of chunks requested but not yet shown
   */
  public int pendingCount() {
    return pending.size();
  }

  /**
   * Returns whether chunks are being repainted for new lighting while the old ones stay shown.
   *
   * @return {@code true} until every chunk around the player has been repainted
   */
  boolean isRelighting() {
    return shownGeneration != targetGeneration;
  }

  @Override
  public Iterator<Renderable> iterator() {
    return drawOrder.iterator();
  }

  /**
   * Paints one chunk and queues it for display, skipping it if newer lighting has been requested
   * since; never lets a failure stop later requests.
   */
  private void paintInBackground(Request request, TerrainLight sun) {
    if (request.generation() != targetGeneration) {
      pending.remove(request);
      return;
    }
    ChunkKey chunk = request.chunk();
    try {
      finished.add(new Painted(request, source.paint(chunk.chunkX(), chunk.chunkY(), sun)));
    } catch (RuntimeException e) {
      pending.remove(request);
      System.err.println("Could not paint terrain chunk " + chunk + ": " + e);
    }
  }

  /**
   * Moves painted chunks into the scene, discarding any the player has since left behind or that
   * were painted for outdated lighting. Chunks painted for new lighting are held back until all
   * of them are ready, unless there is nothing older to show in their place.
   */
  private boolean showFinished(Set<ChunkKey> keep) {
    boolean changed = false;
    Painted painted;
    while ((painted = finished.poll()) != null) {
      Request request = painted.request();
      pending.remove(request);
      ChunkKey chunk = request.chunk();
      if (!keep.contains(chunk)) {
        continue;
      }
      ChunkImage water = painted.image().waterLayer();
      Shown renderable =
          new Shown(
              new ChunkRenderable(painted.image()),
              water == null ? null : new ChunkRenderable(water));
      if (request.generation() == shownGeneration) {
        displayed.put(chunk, renderable);
        changed = true;
      } else if (request.generation() == targetGeneration) {
        staged.put(chunk, renderable);
        if (displayed.putIfAbsent(chunk, renderable) == null) {
          changed = true;
        }
      }
    }
    return changed;
  }

  /**
   * Returns whether two lightings differ enough to be worth repainting for: shadows moved by
   * more than {@link #REPAINT_SHADOW_PX} per level, or a rim's brightness or golden glow changed
   * by more than {@link #REPAINT_RIM_LIGHT}, or sunlight changed by more than {@link #REPAINT_STRENGTH}, or
   * reached full strength or nothing.
   */
  private static boolean noticeablyDifferent(TerrainLight now, TerrainLight painted) {
    if (now.softShadows() != painted.softShadows()
        || now.topGlow() != painted.topGlow()
        || now.faceDarkening() != painted.faceDarkening()) {
      return true;
    }
    double moved =
        Math.abs(now.dxPerLevel() - painted.dxPerLevel())
            + Math.abs(now.dyPerLevel() - painted.dyPerLevel());
    double strengthChange =
        Math.max(
            Math.abs(now.sunlight() - painted.sunlight()),
            Math.abs(now.shadowStrength() - painted.shadowStrength()));
    boolean reachedLimit =
        strengthChange > 0
            && (now.sunlight() == 0 || now.sunlight() == 1 || now.shadowStrength() == 0);
    double rimChange = 0;
    for (Edge edge : Edge.values()) {
      rimChange = Math.max(rimChange, Math.abs(now.edgeLight(edge) - painted.edgeLight(edge)));
      rimChange = Math.max(rimChange, Math.abs(now.edgeGlow(edge) - painted.edgeGlow(edge)));
    }
    return moved > REPAINT_SHADOW_PX
        || rimChange > REPAINT_RIM_LIGHT
        || strengthChange > REPAINT_STRENGTH
        || reachedLimit;
  }

  /**
   * Returns the chunks covering the screen plus {@code border} chunks on every side, nearest the
   * player first. Includes extra room below the screen for tall hills lifted into view.
   */
  private List<ChunkKey> chunksAround(double playerX, double playerY, int border) {
    int minX = toChunk(playerX - viewWidthPx / 2.0) - border;
    int maxX = toChunk(playerX + viewWidthPx / 2.0) + border;
    int minY = toChunk(playerY - viewHeightPx / 2.0 - MAX_LIFT_PX) - border;
    int maxY = toChunk(playerY + viewHeightPx / 2.0) + border;
    int centreX = toChunk(playerX);
    int centreY = toChunk(playerY);
    List<ChunkKey> chunks = new ArrayList<>();
    for (int chunkY = minY; chunkY <= maxY; chunkY++) {
      for (int chunkX = minX; chunkX <= maxX; chunkX++) {
        chunks.add(new ChunkKey(chunkX, chunkY));
      }
    }
    chunks.sort(
        Comparator.comparingInt(
            chunk ->
                Math.abs(chunk.chunkX() - centreX) + Math.abs(chunk.chunkY() - centreY)));
    return chunks;
  }

  /** Returns the chunk containing a world pixel coordinate. */
  private static int toChunk(double px) {
    return Math.floorDiv(TerrainCoordinates.pixelToBlock(px), TerrainCoordinates.CHUNK_SIZE);
  }
}
