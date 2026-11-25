package terrainscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.gameModel.Renderable;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import terrain.OverworldTerrain;

class LightningTerrainTest {

  private static final int VIEW_WIDTH = 800;
  private static final int VIEW_HEIGHT = 600;

  private final List<Runnable> queued = new ArrayList<>();
  private final List<int[]> painted = new ArrayList<>();

  private AreaImage fakePaint(int firstX, int firstY, int width, int height, TerrainLight light) {
    painted.add(new int[] {firstX, firstY, width, height});
    return new AreaImage(
        new BufferedImage(width * 64, height * 64, BufferedImage.TYPE_INT_ARGB),
        firstX * 64,
        firstY * 64);
  }

  private LightningTerrain delayed() {
    return new LightningTerrain(this::fakePaint, queued::add, VIEW_WIDTH, VIEW_HEIGHT);
  }

  private void runQueued() {
    List<Runnable> work = new ArrayList<>(queued);
    queued.clear();
    work.forEach(Runnable::run);
  }

  private static List<Renderable> contents(LightningTerrain lightning) {
    List<Renderable> contents = new ArrayList<>();
    lightning.forEach(contents::add);
    return contents;
  }

  @Test
  void notReadyUntilThePictureIsPainted() {
    LightningTerrain lightning = delayed();
    TerrainLight strike = TerrainLight.lightningStrike(1);
    lightning.prepare(strike, 0, 0);
    assertFalse(lightning.isReadyFor(strike, 0, 0));
    lightning.setShowing(true);
    assertTrue(contents(lightning).isEmpty(), "nothing to show yet");
    runQueued();
    assertTrue(lightning.isReadyFor(strike, 0, 0));
    assertFalse(lightning.isReadyFor(TerrainLight.lightningStrike(2), 0, 0), "another strike");
  }

  @Test
  void preparingTheSameStrikeAgainDoesNotRepaint() {
    LightningTerrain lightning = delayed();
    TerrainLight strike = TerrainLight.lightningStrike(1);
    lightning.prepare(strike, 0, 0);
    lightning.prepare(strike, 0, 0);
    assertEquals(1, queued.size(), "already being painted");
    runQueued();
    lightning.prepare(strike, 30, 20);
    assertTrue(queued.isEmpty(), "already painted, and still covers the screen");
  }

  @Test
  void walkingOutOfThePictureNeedsANewOne() {
    LightningTerrain lightning = delayed();
    TerrainLight strike = TerrainLight.lightningStrike(1);
    lightning.prepare(strike, 0, 0);
    runQueued();
    double withinMargin = (LightningTerrain.MARGIN_BLOCKS - 1) * 64;
    assertTrue(lightning.isReadyFor(strike, withinMargin, 0));
    double beyondMargin = (LightningTerrain.MARGIN_BLOCKS + 1) * 64;
    assertFalse(lightning.isReadyFor(strike, beyondMargin, 0));
    lightning.prepare(strike, beyondMargin, 0);
    assertEquals(1, queued.size(), "painted again around the new position");
  }

  @Test
  void showingCoversTheWholeScreenAndHidingRemovesIt() {
    LightningTerrain lightning = delayed();
    TerrainLight strike = TerrainLight.lightningStrike(1);
    lightning.prepare(strike, 1000, -700);
    runQueued();
    lightning.setShowing(true);
    List<Renderable> shown = contents(lightning);
    assertEquals(1, shown.size());
    Renderable picture = shown.get(0);
    // The game camera's rule, for a player at (1000, -700).
    int screenX = picture.getX() - 1000 + VIEW_WIDTH / 2 - picture.getWidth() / 2;
    int screenY = VIEW_HEIGHT / 2 - (picture.getY() - (-700)) - picture.getHeight();
    assertTrue(screenX <= 0 && screenX + picture.getWidth() >= VIEW_WIDTH);
    assertTrue(screenY <= 0 && screenY + picture.getHeight() >= VIEW_HEIGHT);
    assertTrue(lightning.isShowing());
    lightning.setShowing(false);
    assertTrue(contents(lightning).isEmpty());
    lightning.setShowing(true);
    assertTrue(contents(lightning).get(0) == picture, "the same sprite is reused for a flicker");
  }

  @Test
  void theRealPainterPaintsAnAreaLikeAChunk() {
    ChunkPainter painter =
        new ChunkPainter(OverworldTerrain.fromSeed(42L), new TerrainImages());
    TerrainLight strike = TerrainLight.lightningStrike(0.5);
    ChunkImage chunk = painter.paint(-2, 3, strike);
    AreaImage area = painter.paintArea(-32, 48, 16, 16, strike);
    assertEquals(chunk.bottomPx(), area.bottomPx());
    assertEquals(-2 * 1024, area.leftPx());
    BufferedImage a = chunk.image();
    BufferedImage b = area.image();
    assertEquals(a.getWidth(), b.getWidth());
    assertEquals(a.getHeight(), b.getHeight());
    for (int x = 0; x < a.getWidth(); x += 13) {
      for (int y = 0; y < a.getHeight(); y += 13) {
        assertEquals(a.getRGB(x, y), b.getRGB(x, y));
      }
    }
  }

  @Test
  void aStrikeDarkensTheWaterAndLeavesItSeeThroughInThePicture() {
    OverworldTerrain terrain = OverworldTerrain.fromSeed(42L);
    int[] water = findWater(terrain);
    ChunkPainter painter = new ChunkPainter(terrain, new TerrainImages());
    int firstX = water[0] - 2;
    int firstY = water[1] - 2;
    AreaImage calm = painter.paintArea(firstX, firstY, 5, 5, TerrainLight.DEFAULT);
    AreaImage strike = painter.paintArea(firstX, firstY, 5, 5, TerrainLight.lightningStrike(1));
    // The middle of the water tile, which is the middle of the rectangle.
    int waterX = 2 * 64 + 32;
    int waterY = 2 * 64 + 32;
    int imageY = strike.image().getHeight() - (5 * 64 - waterY);
    assertEquals(
        0, strike.image().getRGB(waterX, imageY) >>> 24, "water must show what is beneath");
    int calmWater = calm.water().getRGB(waterX, waterY);
    int strikeWater = strike.water().getRGB(waterX, waterY);
    assertEquals(255, strikeWater >>> 24);
    assertTrue(brightness(strikeWater) < brightness(calmWater) / 2, "darkened like block sides");
  }

  @Test
  void aStrikeWithWaterShowsTheWaterUnderTheTerrain() {
    LightningTerrain lightning =
        new LightningTerrain(
            (firstX, firstY, width, height, light) ->
                new AreaImage(
                    new BufferedImage(width * 64, height * 64 + 32, BufferedImage.TYPE_INT_ARGB),
                    firstX * 64,
                    firstY * 64,
                    new BufferedImage(width * 64, height * 64, BufferedImage.TYPE_INT_ARGB)),
            Runnable::run,
            VIEW_WIDTH,
            VIEW_HEIGHT);
    lightning.prepare(TerrainLight.lightningStrike(1), 0, 0);
    lightning.setShowing(true);
    List<Renderable> shown = contents(lightning);
    assertEquals(2, shown.size());
    assertEquals(shown.get(0).getY(), shown.get(1).getY(), "both sit on the same ground");
    assertEquals(shown.get(0).getHeight() - 32, shown.get(1).getHeight(), "water is underneath");
  }

  /**
   * Returns a water tile in open water, with no land within 4 tiles, so nothing lifted or no
   * shadow lies over it.
   */
  private static int[] findWater(OverworldTerrain terrain) {
    for (int x = 0; x < 3000; x += 3) {
      for (int y = -300; y <= 300; y += 3) {
        if (allWater(terrain, x, y, 4)) {
          return new int[] {x, y};
        }
      }
    }
    throw new AssertionError("no open water near spawn");
  }

  private static boolean allWater(OverworldTerrain terrain, int x, int y, int reach) {
    for (int east = -reach; east <= reach; east++) {
      for (int north = -reach; north <= reach; north++) {
        if (terrain.heightAt(x + east, y + north) != 0) {
          return false;
        }
      }
    }
    return true;
  }

  private static int brightness(int argb) {
    return ((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF);
  }
}
