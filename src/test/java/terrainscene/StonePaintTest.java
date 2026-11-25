package terrainscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StonePaintTest {

  private static final int SIZE = 16;
  private static final double CENTRE_X = 400;
  private static final double CENTRE_Y = 300;
  private static final double DISTANCE = 400;
  private static final double DEPTH = 64;
  private static final double[] NO_TINT = {1, 1, 1};

  /** Stone whose rows are told apart by their red (the row number) and columns by green. */
  private static int[] markedStone() {
    int[] stone = new int[SIZE * SIZE];
    for (int row = 0; row < SIZE; row++) {
      for (int column = 0; column < SIZE; column++) {
        stone[row * SIZE + column] = 0xFF000000 | row << 16 | column << 8;
      }
    }
    return stone;
  }

  /** A floor's top, along the world row y = 64, seen from a camera at (32, 200). */
  private static StonePaint floorTop(double front, double back) {
    return new StonePaint(markedStone(), SIZE, true, 64, 32, 200, CENTRE_X, CENTRE_Y,
        DISTANCE, DEPTH, front, back, NO_TINT);
  }

  /** Where a point on the floor's top, some depth back and at world x, lands in the image. */
  private static double[] imagePoint(double deep, double worldX) {
    double scale = DISTANCE / (DISTANCE + deep);
    return new double[] {
      CENTRE_X + (worldX - 32) * scale, CENTRE_Y - (64 - 200) * scale
    };
  }

  @Test
  void eachPointShowsTheStoneFromTheRightDepthInPerspective() {
    StonePaint paint = floorTop(1, 1);
    // Half a block deep is the stone's middle row; a quarter deep, the row a quarter down.
    for (double deep : new double[] {8, 16, 33, 47, 60}) {
      double[] at = imagePoint(deep, 40);
      int row = paint.colourAt(at[0], at[1]) >> 16 & 0xFF;
      assertEquals((int) (deep / DEPTH * SIZE), row, "depth " + deep);
    }
  }

  @Test
  void theStoneRepeatsOncePerBlockAlongTheSide() {
    StonePaint paint = floorTop(1, 1);
    // Points in the middle of a stone pixel (4 world pixels wide), not on its edges.
    for (double worldX : new double[] {6, 22, 38, 54, 70, 102, -10}) {
      double[] at = imagePoint(30, worldX);
      int column = paint.colourAt(at[0], at[1]) >> 8 & 0xFF;
      assertEquals(Math.floorMod((int) Math.floor(worldX / 64 * SIZE), SIZE), column,
          "world x " + worldX);
    }
  }

  @Test
  void theFrontIsLitAndTheBackDarker() {
    int[] grey = new int[SIZE * SIZE];
    java.util.Arrays.fill(grey, 0xFF808080);
    StonePaint paint = new StonePaint(grey, SIZE, true, 64, 32, 200, CENTRE_X, CENTRE_Y,
        DISTANCE, DEPTH, 1.4, 0.5, NO_TINT);
    double[] front = imagePoint(1, 32);
    double[] back = imagePoint(63, 32);
    int frontRed = paint.colourAt(front[0], front[1]) >> 16 & 0xFF;
    int backRed = paint.colourAt(back[0], back[1]) >> 16 & 0xFF;
    assertTrue(frontRed > 170 && backRed < 70, "front " + frontRed + ", back " + backRed);
  }

  @Test
  void aWallsSideRunsUpItsColumn() {
    // A wall's side along the world column x = 256, seen from a camera at (32, 200).
    StonePaint paint = new StonePaint(markedStone(), SIZE, false, 256, 32, 200, CENTRE_X,
        CENTRE_Y, DISTANCE, DEPTH, 1, 1, NO_TINT);
    double deep = 32;
    double scale = DISTANCE / (DISTANCE + deep);
    double imageX = CENTRE_X + (256 - 32) * scale;
    double imageY = CENTRE_Y - (210 - 200) * scale;
    int colour = paint.colourAt(imageX, imageY);
    // Across the side is depth, along it is height: half deep is the stone's middle column.
    assertEquals(SIZE / 2, colour >> 8 & 0xFF);
  }
}
