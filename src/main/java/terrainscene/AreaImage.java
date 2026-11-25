package terrainscene;

import java.awt.image.BufferedImage;

/**
 * A rectangle of terrain painted into one image, and where it goes in the world.
 *
 * @param image the painted tiles; taller than the rectangle, since raised tiles are lifted above
 *     its top edge, and see-through above lower tiles, and over water when the water is separate
 * @param leftPx the world x of the image's left edge, in pixels
 * @param bottomPx the world y of the rectangle's bottom edge, in pixels, which is the image's
 *     bottom edge
 * @param water the rectangle's water, exactly covering the rectangle, to draw underneath the
 *     image; {@code null} if there is none or it is part of the image
 */
record AreaImage(BufferedImage image, int leftPx, int bottomPx, BufferedImage water) {

  /**
   * Creates a painted rectangle with no separate water.
   *
   * @param image the painted tiles
   * @param leftPx the world x of the image's left edge, in pixels
   * @param bottomPx the world y of the image's bottom edge, in pixels
   */
  AreaImage(BufferedImage image, int leftPx, int bottomPx) {
    this(image, leftPx, bottomPx, null);
  }
}
