package terrainscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import terrain.Biome;
import terrain.BlockPos;
import terrain.BlockType;
import terrain.Chunk;
import terrain.Terrain;

class WaterPainterTest {

  /** Land west of x = 0 and water to the east of it, everywhere north and south. */
  private static final Terrain SHORE_TO_THE_WEST =
      new Terrain() {
        @Override
        public BlockType blockAt(int blockX, int blockY) {
          return blockX < 0 ? BlockType.GRASS : BlockType.WATER;
        }

        @Override
        public int heightAt(int blockX, int blockY) {
          return blockX < 0 ? 1 : 0;
        }

        @Override
        public Biome biomeAt(int blockX, int blockY) {
          throw new UnsupportedOperationException();
        }

        @Override
        public Chunk chunkAt(int chunkX, int chunkY) {
          throw new UnsupportedOperationException();
        }

        @Override
        public BlockPos spawnPoint() {
          return new BlockPos(0, 0);
        }
      };

  private final WaterPainter painter = new WaterPainter(SHORE_TO_THE_WEST);

  @Test
  void shoreDistanceGrowsAwayFromLandAndStopsAtDeep() {
    assertEquals(0, painter.shoreDistance(-0.5, 3.5), 1e-9);
    assertEquals(0.5, painter.shoreDistance(0.5, 3.5), 1e-9);
    assertEquals(2.25, painter.shoreDistance(2.25, -7), 1e-9);
    assertEquals(WaterPainter.DEEP_BLOCKS, painter.shoreDistance(40, 0), 1e-9);
  }

  @Test
  void waterDarkensFromShoreToDeep() {
    int shore = WaterPainter.colourAt(0, 0);
    int shallow = WaterPainter.colourAt(WaterPainter.SHALLOW_BLOCKS, 0);
    int deep = WaterPainter.colourAt(WaterPainter.DEEP_BLOCKS, 0);
    assertEquals(WaterPainter.SHORE_COLOUR.getRGB(), shore);
    assertEquals(WaterPainter.SHALLOW_COLOUR.getRGB(), shallow);
    assertEquals(WaterPainter.DEEP_COLOUR.getRGB(), deep);
    double previous = Double.MAX_VALUE;
    for (double distance = 0; distance <= WaterPainter.DEEP_BLOCKS; distance += 0.25) {
      double brightness = brightness(WaterPainter.colourAt(distance, 0));
      assertTrue(brightness <= previous, "brighter further out at " + distance);
      previous = brightness;
    }
  }

  @Test
  void landIsTransparentAndWaterIsOpaque() {
    BufferedImage image = painter.paint(-2, 0, 4, 1);
    assertEquals(4 * 64, image.getWidth());
    assertEquals(64, image.getHeight());
    for (int y = 0; y < 64; y++) {
      for (int x = 0; x < 4 * 64; x++) {
        int alpha = image.getRGB(x, y) >>> 24;
        assertEquals(x < 2 * 64 ? 0 : 255, alpha, "at " + x + "," + y);
      }
    }
  }

  @Test
  void colourChangesInTexelSizedSteps() {
    BufferedImage image = painter.paint(0, 0, 1, 1);
    for (int y = 0; y < 64; y++) {
      for (int x = 0; x < 64; x++) {
        int texelLeft = x - x % WaterPainter.TEXEL_PX;
        int texelTop = y - y % WaterPainter.TEXEL_PX;
        assertEquals(image.getRGB(texelLeft, texelTop), image.getRGB(x, y));
      }
    }
  }

  @Test
  void paintingTileByTileMatchesPaintingAnArea() {
    BufferedImage area = painter.paint(-1, -3, 5, 4);
    for (int tileX = 0; tileX < 5; tileX++) {
      for (int tileY = 0; tileY < 4; tileY++) {
        BufferedImage tile = painter.paint(-1 + tileX, -3 + tileY, 1, 1);
        // The area's top row is its northernmost tiles.
        int top = (3 - tileY) * 64;
        for (int y = 0; y < 64; y++) {
          for (int x = 0; x < 64; x++) {
            int expected = tile == null ? 0 : tile.getRGB(x, y);
            assertEquals(expected, area.getRGB(tileX * 64 + x, top + y));
          }
        }
      }
    }
  }

  @Test
  void openWaterIsDeepButFaintlyTextured() {
    BufferedImage image = painter.paint(30, 0, 1, 1);
    assertNotNull(image);
    int lightest = 0;
    int darkest = 255;
    for (int y = 0; y < 64; y += WaterPainter.TEXEL_PX) {
      for (int x = 0; x < 64; x += WaterPainter.TEXEL_PX) {
        int blue = image.getRGB(x, y) & 0xFF;
        lightest = Math.max(lightest, blue);
        darkest = Math.min(darkest, blue);
        assertEquals(WaterPainter.DEEP_COLOUR.getBlue(), blue, 6);
      }
    }
    assertTrue(lightest > darkest, "open water should not be one flat colour");
  }

  @Test
  void dryLandHasNoWaterImage() {
    assertNull(painter.paint(-20, 0, 16, 16));
  }

  private static double brightness(int rgb) {
    return ((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF);
  }
}
