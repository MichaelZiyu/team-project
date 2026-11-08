package terrainscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.gameModel.Renderable;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ChunkedTerrainSceneTest {

  private static final int VIEW_WIDTH = 800;
  private static final int VIEW_HEIGHT = 600;
  private static final int CHUNK_PX = 1024;

  /** How often each chunk was painted, keyed "x,y". */
  private final Map<String, Integer> paintCounts = new HashMap<>();

  /** Work handed to the "background thread"; run when the test says so. */
  private final List<Runnable> queued = new ArrayList<>();

  private ChunkImage fakePaint(int chunkX, int chunkY) {
    paintCounts.merge(chunkX + "," + chunkY, 1, Integer::sum);
    return new ChunkImage(
        chunkX, chunkY, new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB), chunkY * CHUNK_PX);
  }

  /** A scene that paints immediately, as if the background thread were instant. */
  private ChunkedTerrainScene instantScene(int renderDistance) {
    return new ChunkedTerrainScene(
        this::fakePaint, Runnable::run, VIEW_WIDTH, VIEW_HEIGHT, renderDistance);
  }

  /** A scene whose background work waits until {@link #runQueued()} is called. */
  private ChunkedTerrainScene delayedScene() {
    return new ChunkedTerrainScene(this::fakePaint, queued::add, VIEW_WIDTH, VIEW_HEIGHT, 1);
  }

  private void runQueued() {
    List<Runnable> work = new ArrayList<>(queued);
    queued.clear();
    work.forEach(Runnable::run);
  }

  private static List<ChunkRenderable> chunks(ChunkedTerrainScene scene) {
    List<ChunkRenderable> chunks = new ArrayList<>();
    for (Renderable piece : scene) {
      chunks.add((ChunkRenderable) piece);
    }
    return chunks;
  }

  /** Returns how many chunks a view needs, with tall-hill room below, plus a border. */
  private static int expectedChunks(double playerX, double playerY, int border) {
    int minX = Math.floorDiv((int) Math.floor(playerX - VIEW_WIDTH / 2.0), CHUNK_PX) - border;
    int maxX = Math.floorDiv((int) Math.floor(playerX + VIEW_WIDTH / 2.0), CHUNK_PX) + border;
    double bottom = playerY - VIEW_HEIGHT / 2.0 - ChunkedTerrainScene.MAX_LIFT_PX;
    int minY = Math.floorDiv((int) Math.floor(bottom), CHUNK_PX) - border;
    int maxY = Math.floorDiv((int) Math.floor(playerY + VIEW_HEIGHT / 2.0), CHUNK_PX) + border;
    return (maxX - minX + 1) * (maxY - minY + 1);
  }

  @Test
  void coversTheScreenPlusTheBorder() {
    for (int border = 0; border <= 2; border++) {
      paintCounts.clear();
      ChunkedTerrainScene scene = instantScene(border);
      scene.update(500, 500);
      assertEquals(expectedChunks(500, 500, border), scene.displayedCount(), "border " + border);
      assertEquals(scene.displayedCount(), paintCounts.size());
    }
  }

  @Test
  void nearerChunksComeFirst() {
    ChunkedTerrainScene scene = instantScene(1);
    scene.update(0, 0);
    int lastRow = Integer.MIN_VALUE;
    for (ChunkRenderable chunk : chunks(scene)) {
      assertTrue(chunk.getChunkY() >= lastRow, "chunks must go from near (low y) to far");
      lastRow = chunk.getChunkY();
    }
  }

  @Test
  void movingWithinTheBorderPaintsNothingNew() {
    ChunkedTerrainScene scene = instantScene(1);
    scene.update(0, 0);
    int painted = paintCounts.size();
    // Half a chunk to the right stays inside what the border already prepared.
    scene.update(CHUNK_PX / 2.0, 0);
    scene.update(-CHUNK_PX / 4.0, 100);
    assertEquals(painted, paintCounts.size());
    paintCounts.values().forEach(count -> assertEquals(1, count));
  }

  @Test
  void walkingFarPaintsAheadAndDropsChunksLeftBehind() {
    ChunkedTerrainScene scene = instantScene(1);
    scene.update(0, 0);
    for (int step = 1; step <= 40; step++) {
      scene.update(step * 256.0, 0);
    }
    // Everything within the border is ready; chunks one step further are kept, not repainted.
    int displayed = scene.displayedCount();
    assertTrue(displayed >= expectedChunks(40 * 256.0, 0, 1), "missing chunks: " + displayed);
    assertTrue(displayed <= expectedChunks(40 * 256.0, 0, 2), "kept too many: " + displayed);
    assertTrue(paintCounts.containsKey("11,0"), "chunks ahead should have been painted");
    for (ChunkRenderable chunk : chunks(scene)) {
      assertTrue(chunk.getChunkX() >= 7, "chunk " + chunk.getChunkX() + " should have been dropped");
    }
  }

  @Test
  void chunksAppearOnceTheBackgroundWorkFinishes() {
    ChunkedTerrainScene scene = delayedScene();
    scene.update(0, 0);
    assertEquals(0, scene.displayedCount());
    assertEquals(expectedChunks(0, 0, 1), scene.pendingCount());
    // Asking again while painting is under way must not request the same chunks twice.
    scene.update(10, 10);
    assertEquals(expectedChunks(0, 0, 1), queued.size());
    runQueued();
    scene.update(10, 10);
    assertEquals(expectedChunks(0, 0, 1), scene.displayedCount());
    assertEquals(0, scene.pendingCount());
  }

  @Test
  void failedChunksAreRetriedLater() {
    boolean[] fail = {true};
    ChunkedTerrainScene scene =
        new ChunkedTerrainScene(
            (x, y) -> {
              if (fail[0]) {
                throw new IllegalStateException("simulated failure");
              }
              return fakePaint(x, y);
            },
            Runnable::run,
            VIEW_WIDTH,
            VIEW_HEIGHT,
            0);
    scene.update(0, 0);
    assertEquals(0, scene.displayedCount());
    assertEquals(0, scene.pendingCount());
    fail[0] = false;
    scene.update(0, 0);
    assertEquals(expectedChunks(0, 0, 0), scene.displayedCount());
  }

  @Test
  void negativeRenderDistanceIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> instantScene(-1));
  }

  @Test
  void chunkSpritePositionMatchesTheRenderersConvention() {
    ChunkRenderable chunk =
        new ChunkRenderable(
            new ChunkImage(-1, 2, new BufferedImage(1024, 1280, BufferedImage.TYPE_INT_ARGB), 2048));
    assertEquals(-1024 + 512, chunk.getX());
    assertEquals(2048, chunk.getY());
    assertEquals(1024, chunk.getWidth());
    assertEquals(1280, chunk.getHeight());
    assertEquals(1024, chunk.getTexture().getWidth());
    assertEquals(1280, chunk.getTexture().getContent().getIcon().getIconHeight());
  }
}
