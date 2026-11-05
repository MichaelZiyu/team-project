package terrainscene;

import daynight.SunPosition;
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
import terrain.OverworldTerrain;
import terrain.Terrain;
import terrain.TerrainCoordinates;

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
 * <p>Call {@link #update} and iterate on the Swing thread (the game's render step does both). The
 * renderer draws the first sprite on top, so chunks nearest the camera come first.
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
    ChunkImage paint(int chunkX, int chunkY, ShadowCast sun);
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

  /** How many chunks are prepared beyond each edge of the screen unless told otherwise. */
  public static final int DEFAULT_RENDER_DISTANCE = 1;

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

  /** Chunks painted on background threads and waiting to be shown. */
  private final Queue<Painted> finished = new ConcurrentLinkedQueue<>();

  /** Chunks being painted right now, so they are not requested twice. */
  private final Set<Request> pending = ConcurrentHashMap.newKeySet();

  /** Chunks ready to draw; only touched on the Swing thread. */
  private final Map<ChunkKey, ChunkRenderable> displayed = new HashMap<>();

  /** Chunks already painted for new lighting, waiting until all are ready to be swapped in. */
  private final Map<ChunkKey, ChunkRenderable> staged = new HashMap<>();

  /** The lighting generation of the chunks on screen. */
  private int shownGeneration;

  /** The newest lighting generation; read by painting threads to skip outdated work. */
  private volatile int targetGeneration;

  /** The newest lighting, or {@code null} before the first update. */
  private ShadowCast targetSun;

  private List<Renderable> drawOrder = List.of();

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
        renderDistance);
  }

  /**
   * Creates a scene with a given way of painting and running work; used by tests.
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
    if (renderDistance < 0) {
      throw new IllegalArgumentException("renderDistance must not be negative: " + renderDistance);
    }
    this.source = source;
    this.executor = executor;
    this.viewWidthPx = viewWidthPx;
    this.viewHeightPx = viewHeightPx;
    this.renderDistance = renderDistance;
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
    ChunkSource painter = new ChunkPainter(terrain, new TerrainImages());
    return new ChunkedTerrainScene(
        painter, newPaintingThreads(), viewWidthPx, viewHeightPx, renderDistance);
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
   * Brings the scene up to date for the player's position, lit by the default sun.
   *
   * @param playerX the player's x position, in pixels; the camera is centred on it
   * @param playerY the player's y position, in pixels
   * @see #update(double, double, ShadowCast)
   */
  public void update(double playerX, double playerY) {
    update(playerX, playerY, ShadowCast.DEFAULT);
  }

  /**
   * Brings the scene up to date for the player's position and the sun's position.
   *
   * @param playerX the player's x position, in pixels; the camera is centred on it
   * @param playerY the player's y position, in pixels
   * @param sun where the sun is
   * @see #update(double, double, ShadowCast)
   */
  public void update(double playerX, double playerY, SunPosition sun) {
    update(playerX, playerY, ShadowCast.from(sun));
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
  public void update(double playerX, double playerY, ShadowCast sun) {
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
    ShadowCast lighting = targetSun;
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
      List<ChunkRenderable> ordered = new ArrayList<>(displayed.values());
      ordered.sort(
          Comparator.comparingInt(ChunkRenderable::getChunkY)
              .thenComparingInt(ChunkRenderable::getChunkX));
      drawOrder = Collections.unmodifiableList(new ArrayList<>(ordered));
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
  private void paintInBackground(Request request, ShadowCast sun) {
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
      ChunkRenderable renderable = new ChunkRenderable(painted.image());
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
   * more than {@link #REPAINT_SHADOW_PX} per level, or a rim's brightness changed by more than
   * {@link #REPAINT_RIM_LIGHT}, or sunlight changed by more than {@link #REPAINT_STRENGTH}, or
   * reached full strength or nothing.
   */
  private static boolean noticeablyDifferent(ShadowCast now, ShadowCast painted) {
    double moved =
        Math.abs(now.dxPerLevel() - painted.dxPerLevel())
            + Math.abs(now.dyPerLevel() - painted.dyPerLevel());
    double strengthChange = Math.abs(now.strength() - painted.strength());
    boolean reachedLimit =
        strengthChange > 0 && (now.strength() == 0 || now.strength() == 1);
    double rimChange = 0;
    for (Edge edge : Edge.values()) {
      rimChange = Math.max(rimChange, Math.abs(now.edgeLight(edge) - painted.edgeLight(edge)));
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
