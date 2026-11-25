package terrainscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import terrain.BlockType;

class RimGlowTest {

  private static int paintedColour(RimRenderable rim) {
    BufferedImage image =
        new BufferedImage(rim.getWidth(), rim.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    rim.paintOnto(g, 0, 0, new TerrainImages());
    g.dispose();
    return image.getRGB(rim.getWidth() / 2, rim.getHeight() / 2);
  }

  @Test
  void aFullyGildedRimIsGoldenOrangeYellow() {
    RimRenderable rim = new RimRenderable(0, 0, BlockType.GRASS, 1, Edge.LEFT, 0.2, 1);
    assertEquals(RimRenderable.GOLDEN.getRGB(), paintedColour(rim));
  }

  @Test
  void aPartlyGildedRimLeansOrange() {
    int plain = paintedColour(new RimRenderable(0, 0, BlockType.GRASS, 1, Edge.LEFT, 0.2, 0));
    int half = paintedColour(new RimRenderable(0, 0, BlockType.GRASS, 1, Edge.LEFT, 0.2, 0.5));
    assertTrue((half >> 16 & 0xFF) > (plain >> 16 & 0xFF), "redder");
    assertTrue((half & 0xFF) < 0xC0, "not blue-white");
  }

  @Test
  void glowMustBeBetweenZeroAndOne() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RimRenderable(0, 0, BlockType.GRASS, 1, Edge.LEFT, 0.2, 1.5));
  }
}
