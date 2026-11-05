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

  /** The lighting each chunk image was painted with. */
  private final Map<BufferedImage, ShadowCast> paintedWith = new HashMap<>();

  /** Work handed to the "background thread"; run when the test says so. */
  private final List<Runnable> queued = new ArrayList<>();

  private ChunkImage fakePaint(int chunkX, int chunkY, ShadowCast sun) {
    paintCounts.merge(chunkX + "," + chunkY, 1, Integer::sum);
    BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
    paintedWith.put(image, sun);
    return new ChunkImage(chunkX, chunkY, image, chunkY * CHUNK_PX);
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

  /** Returns the image a chunk sprite shows. */
  private static BufferedImage imageOf(ChunkRenderable chunk) {
    javax.swing.Icon icon = chunk.getTexture().getContent().getIcon();
    return (BufferedImage) ((javax.swing.ImageIcon) icon).getImage();
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
            (x, y, sun) -> {
              if (fail[0]) {
                throw new IllegalStateException("simulated failure");
              }
              return fakePaint(x, y, sun);
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
  void movingSunRepaintsEveryChunkAndSwapsThemAllAtOnce() {
    ChunkedTerrainScene scene = delayedScene();
    scene.update(0, 0, ShadowCast.DEFAULT);
    runQueued();
    scene.update(0, 0, ShadowCast.DEFAULT);
    int shown = scene.displayedCount();
    ShadowCast later = new ShadowCast(-20, -30, 1);
    scene.update(0, 0, later);
    assertTrue(scene.isRelighting());
    assertEquals(shown, queued.size(), "every chunk around the player is repainted");
    // Finish all but one: the old images must all stay, so no seam shows between lightings.
    Runnable last = queued.remove(queued.size() - 1);
    runQueued();
    scene.update(0, 0, later);
    assertTrue(scene.isRelighting());
    for (ChunkRenderable chunk : chunks(scene)) {
      assertEquals(ShadowCast.DEFAULT, paintedWith.get(imageOf(chunk)));
    }
    last.run();
    scene.update(0, 0, later);
    assertTrue(!scene.isRelighting());
    assertEquals(shown, scene.displayedCount());
    for (ChunkRenderable chunk : chunks(scene)) {
      assertEquals(later, paintedWith.get(imageOf(chunk)));
    }
  }

  @Test
  void tinySunMovementsDoNotRepaint() {
    ChunkedTerrainScene scene = instantScene(0);
    scene.update(0, 0, ShadowCast.DEFAULT);
    int painted = paintCounts.values().stream().mapToInt(Integer::intValue).sum();
    scene.update(0, 0, new ShadowCast(-31, -15, 0.95));
    assertEquals(painted, paintCounts.values().stream().mapToInt(Integer::intValue).sum());
    scene.update(0, 0, ShadowCast.NIGHT);
    assertEquals(2 * painted, paintCounts.values().stream().mapToInt(Integer::intValue).sum());
    // Night stays night: nothing to repaint until the sun comes up.
    scene.update(0, 0, ShadowCast.NIGHT);
    assertEquals(2 * painted, paintCounts.values().stream().mapToInt(Integer::intValue).sum());
  }

  @Test
  void aRepaintFinishesBeforeTheNextOneStartsFromTheLatestSun() {
    ChunkedTerrainScene scene = delayedScene();
    scene.update(0, 0, ShadowCast.DEFAULT);
    runQueued();
    scene.update(0, 0, ShadowCast.DEFAULT);
    ShadowCast first = new ShadowCast(-20, -30, 1);
    scene.update(0, 0, first);
    int requests = queued.size();
    // The sun keeps moving while the first repaint is under way: nothing new is requested yet.
    scene.update(0, 0, new ShadowCast(0, -30, 1));
    ShadowCast latest = new ShadowCast(20, -30, 1);
    scene.update(0, 0, latest);
    assertEquals(requests, queued.size());
    runQueued();
    scene.update(0, 0, latest);
    for (ChunkRenderable chunk : chunks(scene)) {
      assertEquals(first, paintedWith.get(imageOf(chunk)), "the first repaint is shown");
    }
    // On the next frame the next repaint starts, straight for the latest sun.
    scene.update(0, 0, latest);
    assertTrue(scene.isRelighting());
    assertEquals(requests, queued.size());
    runQueued();
    scene.update(0, 0, latest);
    assertTrue(!scene.isRelighting());
    for (ChunkRenderable chunk : chunks(scene)) {
      assertEquals(latest, paintedWith.get(imageOf(chunk)));
    }
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
