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

/**
 * The terrain around the player as a handful of pre-painted chunk images, prepared ahead of the
 * camera so walking stays smooth.
 *
 * <p>The scene keeps every chunk that is on screen, plus a border of {@code renderDistance}
 * chunks around it, painted and ready. As the camera moves, chunks entering that border are
 * painted on a background thread and appear once ready; chunks well outside it are dropped to
 * save memory. Because the border is prepared in advance, the edge of the screen should reach a
 * chunk only after it has been painted.
 *
 * <p>Call {@link #update} and iterate on the Swing thread (the game's render step does both). The
 * renderer draws the first sprite on top, so chunks nearest the camera come first.
 */
public final class ChunkedTerrainScene implements Iterable<Renderable> {

  /** Paints one chunk; lets tests replace painting with something simple. */
  @FunctionalInterface
  interface ChunkSource {

    /**
     * Paints a chunk. May be called on a background thread.
     *
     * @param chunkX the chunk column, in chunks
     * @param chunkY the chunk row, in chunks
     * @return the painted chunk
     */
    ChunkImage paint(int chunkX, int chunkY);
  }

  /** A chunk's position, in chunks. */
  private record ChunkKey(int chunkX, int chunkY) {}

  /** How many chunks are prepared beyond each edge of the screen unless told otherwise. */
  public static final int DEFAULT_RENDER_DISTANCE = World.TERRAIN_RENDER_DISTANCE;

  /**
   * How far raised tiles can be lifted into view from below the screen, in pixels: 16 height
   * levels. Chunks that far below the screen are included so tall hills never pop in.
   */
  static final int MAX_LIFT_PX = 16 * TileRenderable.STEP_HEIGHT_PX;

  private static final int CHUNK_PX =
      TerrainCoordinates.CHUNK_SIZE * TerrainCoordinates.BLOCK_SIZE_PX;

  private final ChunkSource source;
  private final Executor executor;
  private final int viewWidthPx;
  private final int viewHeightPx;
  private final int renderDistance;

  /** Chunks painted on the background thread and waiting to be shown. */
  private final Queue<ChunkImage> finished = new ConcurrentLinkedQueue<>();

  /** Chunks being painted right now, so they are not requested twice. */
  private final Set<ChunkKey> pending = ConcurrentHashMap.newKeySet();

  /** Chunks ready to draw; only touched on the Swing thread. */
  private final Map<ChunkKey, ChunkRenderable> displayed = new HashMap<>();

  private List<Renderable> drawOrder = List.of();

  /**
   * Creates a scene that paints the terrain generated from the given seed
   * on its own background thread.
   *
   * @param seed the seed used to generate the terrain
   */
  public ChunkedTerrainScene(long seed) {
    this(
            new ChunkPainter(
                    OverworldTerrain.fromSeed(seed),
                    new TerrainImages()),
            Executors.newSingleThreadExecutor(
                    task -> {
                      Thread thread = new Thread(task, "terrain-chunks");
                      thread.setDaemon(true);
                      return thread;
                    }),
            WindowManager.FRAME_WIDTH,
            WindowManager.FRAME_HEIGHT,
            World.TERRAIN_RENDER_DISTANCE);
  }

  /**
   * Creates a scene with a given way of painting and running work; used by tests.
   *
   * @param source paints each chunk
   * @param executor runs painting work, normally on a background thread
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
   * Brings the scene up to date for the player's position: shows chunks that finished painting,
   * drops chunks far off screen, and starts painting chunks entering the border.
   *
   * @param playerX the player's x position, in pixels; the camera is centred on it
   * @param playerY the player's y position, in pixels
   */
  public void update(double playerX, double playerY) {
    List<ChunkKey> wanted = chunksAround(playerX, playerY, renderDistance);
    Set<ChunkKey> keep = new LinkedHashSet<>(chunksAround(playerX, playerY, renderDistance + 1));
    boolean changed = showFinished(keep);
    changed |= displayed.keySet().retainAll(keep);
    for (ChunkKey chunk : wanted) {
      if (!displayed.containsKey(chunk) && pending.add(chunk)) {
        executor.execute(() -> paintInBackground(chunk));
      }
    }
    changed |= showFinished(keep);
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

  @Override
  public Iterator<Renderable> iterator() {
    return drawOrder.iterator();
  }

  /** Paints one chunk and queues it for display; never lets a failure stop later requests. */
  private void paintInBackground(ChunkKey chunk) {
    try {
      finished.add(source.paint(chunk.chunkX(), chunk.chunkY()));
    } catch (RuntimeException e) {
      pending.remove(chunk);
      System.err.println("Could not paint terrain chunk " + chunk + ": " + e);
    }
  }

  /** Moves painted chunks into the scene, discarding any the player has since left behind. */
  private boolean showFinished(Set<ChunkKey> keep) {
    boolean changed = false;
    ChunkImage chunk;
    while ((chunk = finished.poll()) != null) {
      ChunkKey position = new ChunkKey(chunk.chunkX(), chunk.chunkY());
      pending.remove(position);
      if (keep.contains(position)) {
        displayed.put(position, new ChunkRenderable(chunk));
        changed = true;
      }
    }
    return changed;
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
