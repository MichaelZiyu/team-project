package terrainscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class WaterRipplesTest {

  private static final WaterRipples RIPPLES = WaterRipples.create(1L);

  @Test
  void theLoopStartsOverAfterItsLength() {
    assertSame(RIPPLES.frameAt(0.1), RIPPLES.frameAt(0.1 + WaterRipples.LOOP_SECONDS));
    assertSame(RIPPLES.frame(0), RIPPLES.frameAt(0));
    assertSame(
        RIPPLES.frame(WaterRipples.FRAMES - 1),
        RIPPLES.frameAt(WaterRipples.LOOP_SECONDS - 1e-6));
  }

  @Test
  void framesAreTilesThatChangeOverTime() {
    BufferedImage first = RIPPLES.frame(0);
    BufferedImage later = RIPPLES.frame(WaterRipples.FRAMES / 4);
    assertEquals(WaterRipples.TILE_PX, first.getWidth());
    assertEquals(WaterRipples.TILE_PX, first.getHeight());
    int changed = 0;
    for (int y = 0; y < WaterRipples.TILE_PX; y++) {
      for (int x = 0; x < WaterRipples.TILE_PX; x++) {
        if (first.getRGB(x, y) != later.getRGB(x, y)) {
          changed++;
        }
      }
    }
    assertTrue(changed > WaterRipples.TILE_PX * WaterRipples.TILE_PX / 4, "barely moved");
  }

  @Test
  void tilesJoinWithoutASeam() {
    // Where one tile meets the next should differ no more than neighbouring steps inside a tile.
    BufferedImage frame = RIPPLES.frame(5);
    int step = WaterRipples.TEXEL_PX;
    int inside = 0;
    for (int column = step; column < WaterRipples.TILE_PX; column += step) {
      inside = Math.max(inside, edgeDifference(frame, column - 1, column));
    }
    assertTrue(edgeDifference(frame, WaterRipples.TILE_PX - 1, 0) <= inside);
  }

  @Test
  void ripplesAreOnlyHighlightsWithTheWatersColourBetweenThem() {
    BufferedImage frame = RIPPLES.frame(0);
    int clear = 0;
    int lit = 0;
    for (int y = 0; y < WaterRipples.TILE_PX; y++) {
      for (int x = 0; x < WaterRipples.TILE_PX; x++) {
        int argb = frame.getRGB(x, y);
        if (argb >>> 24 == 0) {
          clear++;
        } else {
          assertEquals(WaterRipples.LIGHT_RGB, argb & 0xFFFFFF, "only highlights, no shade");
          lit++;
        }
      }
    }
    assertTrue(clear > 0 && lit > 0);
  }

  @Test
  void onlySteepSlopesFacingTheCameraAreLit() {
    assertEquals(0, WaterRipples.shade(0));
    assertEquals(0, WaterRipples.shade(WaterRipples.THRESHOLD));
    assertEquals(0, WaterRipples.shade(-WaterRipples.FULL), "slopes facing away stay clear");
    int lit = WaterRipples.shade(WaterRipples.FULL);
    assertEquals(WaterRipples.LIGHT_RGB, lit & 0xFFFFFF);
    assertEquals(Math.round(WaterRipples.LIGHT_OPACITY * 255), lit >>> 24);
  }

  /** Returns how many pixels differ between two columns. */
  private static int edgeDifference(BufferedImage frame, int columnA, int columnB) {
    int different = 0;
    for (int y = 0; y < frame.getHeight(); y++) {
      if (frame.getRGB(columnA, y) != frame.getRGB(columnB, y)) {
        different++;
      }
    }
    return different;
  }
}
