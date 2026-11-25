package terrainscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import terrain.Tree;

class TreePainterTest {

  private final TreePainter painter = new TreePainter();

  @Test
  void pictureIsAsWideAsTheCanopyAndAsTallAsTheTree() {
    for (Tree.Shape shape : Tree.Shape.values()) {
      BufferedImage image = painter.paint(shape, 1);
      assertEquals((2 * shape.radius() + 1) * 64, image.getWidth(), shape.name());
      assertEquals(TreePainter.bounds(shape)[1], image.getHeight(), shape.name());
    }
  }

  @Test
  void theTrunkShowsAtTheBottomMiddle() {
    for (Tree.Shape shape : Tree.Shape.values()) {
      BufferedImage image = painter.paint(shape, 1);
      int bottomOfTrunk = image.getHeight() - 1 + TreePainter.bounds(shape)[0];
      int argb = image.getRGB(image.getWidth() / 2, bottomOfTrunk - 4);
      int red = argb >> 16 & 0xFF;
      int green = argb >> 8 & 0xFF;
      assertEquals(255, argb >>> 24, shape.name());
      assertTrue(red >= green, shape + " shows bark, not leaves, at the trunk's foot");
    }
  }

  @Test
  void theCornersBesideTheTrunkAreSeeThrough() {
    BufferedImage image = painter.paint(Tree.Shape.SMALL, 1);
    assertEquals(0, image.getRGB(0, image.getHeight() - 1) >>> 24);
  }

  @Test
  void picturesAreKeptAndReused() {
    assertSame(painter.paint(Tree.Shape.TALL, 0.55), painter.paint(Tree.Shape.TALL, 0.55));
  }

  @Test
  void fadedPictureIsSeeThrough() {
    BufferedImage solid = painter.paint(Tree.Shape.TALL, 1);
    BufferedImage faded = painter.paint(Tree.Shape.TALL, 0.5);
    // A solid pixel, not one of the gaps in the leaves: the bottom of the trunk.
    int x = solid.getWidth() / 2;
    int y = solid.getHeight() - 1 + TreePainter.bounds(Tree.Shape.TALL)[0] - 4;
    assertEquals(255, solid.getRGB(x, y) >>> 24);
    assertEquals(128, faded.getRGB(x, y) >>> 24, 2);
  }
}
