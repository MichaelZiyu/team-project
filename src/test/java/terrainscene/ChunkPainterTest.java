package terrainscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.gameModel.Renderable;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import jswing.Renderer;
import org.junit.jupiter.api.Test;
import terrain.OverworldTerrain;
import terrain.Terrain;

class ChunkPainterTest {

  private static final int VIEW_WIDTH = 800;
  private static final int VIEW_HEIGHT = 600;

  private final Terrain terrain = OverworldTerrain.fromSeed(42L);

  @Test
  void chunkImageIsTallEnoughForItsHighestTile() {
    ChunkImage chunk = new ChunkPainter(terrain, new TerrainImages()).paint(-2, 3, ShadowCast.DEFAULT);
    int maxHeight = 0;
    for (int x = -32; x < -16; x++) {
      for (int y = 48; y < 64; y++) {
        maxHeight = Math.max(maxHeight, terrain.heightAt(x, y));
      }
    }
    assertEquals(1024, chunk.image().getWidth());
    assertEquals(1024 + maxHeight * TileRenderable.STEP_HEIGHT_PX, chunk.image().getHeight());
    assertEquals(-2, chunk.chunkX());
    assertEquals(3, chunk.chunkY());
    assertEquals(3 * 1024, chunk.bottomPx());
  }

  @Test
  void chunkImagesLookTheSameAsDrawingEveryPiece() {
    // Several spots, including chunk corners and negative coordinates.
    double[][] players = {{0, 0}, {-3000.5, 4321}, {1024, -1024}, {-46_800, 26_100}};
    for (double[] player : players) {
      BufferedImage pieces = render(piecesScene(), player[0], player[1]);
      BufferedImage chunks = render(chunkScene(), player[0], player[1]);
      int different = 0;
      for (int x = 0; x < VIEW_WIDTH; x++) {
        for (int y = 0; y < VIEW_HEIGHT; y++) {
          if (!sameColour(pieces.getRGB(x, y), chunks.getRGB(x, y))) {
            different++;
          }
        }
      }
      assertEquals(0, different, "pixels differ at player " + player[0] + "," + player[1]);
    }
  }

  @Test
  void chunkImagesMatchPiecesAtOtherTimesOfDay() {
    // Evening (shadows to the right, left rims lit) and late afternoon at half strength.
    ShadowCast[] suns = {
      ShadowCast.from(daynight.SunPosition.at(0.68)),
      new ShadowCast(24, -10, 0.5),
      ShadowCast.NIGHT
    };
    for (ShadowCast sun : suns) {
      TerrainScene pieces = piecesScene();
      pieces.update(-46_800, 26_100, sun);
      ChunkedTerrainScene chunks = chunkScene();
      chunks.update(-46_800, 26_100, sun);
      BufferedImage expected = draw(pieces, -46_800, 26_100);
      BufferedImage actual = draw(chunks, -46_800, 26_100);
      int different = 0;
      for (int x = 0; x < VIEW_WIDTH; x++) {
        for (int y = 0; y < VIEW_HEIGHT; y++) {
          if (!sameColour(expected.getRGB(x, y), actual.getRGB(x, y))) {
            different++;
          }
        }
      }
      assertEquals(0, different, "pixels differ for " + sun);
    }
  }

  @Test
  void raisedTilesLeaveTransparentSpaceAboveLowerOnes() {
    ChunkImage chunk = new ChunkPainter(terrain, new TerrainImages()).paint(0, 0, ShadowCast.DEFAULT);
    BufferedImage image = chunk.image();
    int transparent = 0;
    for (int x = 0; x < image.getWidth(); x += 4) {
      if ((image.getRGB(x, 0) >>> 24) == 0) {
        transparent++;
      }
    }
    // The top row of pixels only holds the tallest tiles; everywhere else is see-through.
    assertTrue(transparent > 0, "expected see-through space above lower tiles");
  }

  private TerrainScene piecesScene() {
    return TerrainScene.withPlaceholderTiles(terrain, VIEW_WIDTH, VIEW_HEIGHT);
  }

  private ChunkedTerrainScene chunkScene() {
    // Paint on the calling thread so every chunk is ready after one update.
    return new ChunkedTerrainScene(
        new ChunkPainter(terrain, new TerrainImages()), Runnable::run, VIEW_WIDTH, VIEW_HEIGHT, 0);
  }

  /** Draws a scene through the game's real renderer into an image, centred on the player. */
  private static BufferedImage render(Iterable<Renderable> scene, double playerX, double playerY) {
    if (scene instanceof TerrainScene pieces) {
      pieces.update(playerX, playerY);
    } else if (scene instanceof ChunkedTerrainScene chunks) {
      chunks.update(playerX, playerY);
    }
    return draw(scene, playerX, playerY);
  }

  /** Draws an already updated scene through the game's real renderer into an image. */
  private static BufferedImage draw(Iterable<Renderable> scene, double playerX, double playerY) {
    JPanel panel = new JPanel();
    panel.setLayout(null);
    panel.setSize(VIEW_WIDTH, VIEW_HEIGHT);
    new Renderer(panel).render(scene, (int) playerX, (int) playerY);
    BufferedImage image = new BufferedImage(VIEW_WIDTH, VIEW_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    panel.paint(image.getGraphics());
    return image;
  }

  /** Treats colours within one step per channel as equal, to allow for rounding in blending. */
  private static boolean sameColour(int first, int second) {
    for (int shift = 0; shift <= 24; shift += 8) {
      int a = (first >>> shift) & 0xFF;
      int b = (second >>> shift) & 0xFF;
      if (Math.abs(a - b) > 1) {
        return false;
      }
    }
    return true;
  }
}
