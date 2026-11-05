package terrainscene;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import jswing.Texture;
import terrain.TerrainCoordinates;

/**
 * A see-through warm overlay on a tile's top, so ground in the sun looks golden next to the cool
 * blue of shadows and terrace faces.
 *
 * <p>Every tile top gets one during the day, under its shadow, so shaded parts end up cool and
 * the rest warm. It fades with the sunlight toward dawn and dusk. Like {@link
 * FaceShadeRenderable} it is an overlay rather than a tinted copy of the tile image, so it works
 * the same with placeholder images and with real textures.
 */
public final class SunlightRenderable implements TerrainPiece {

  /** The overlay's colour in full sunlight: a light golden wash at about 12% opacity. */
  static final Color SUNLIGHT_COLOUR = new Color(255, 196, 120, 30);

  private static final long serialVersionUID = 1L;

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  private final int blockX;
  private final int blockY;
  private final int height;
  private final Color colour;

  /** Made on first use, and never saved: pieces are rebuilt from terrain. */
  private transient Texture texture;

  /**
   * Creates the overlay for one tile's top.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @param height the tile's terrace height, so the overlay sits on its top
   * @param strength how strong sunlight is, from 0 to 1; the overlay is this fraction as strong
   * @throws IllegalArgumentException if {@code strength} is out of range
   */
  public SunlightRenderable(int blockX, int blockY, int height, double strength) {
    if (!(strength >= 0 && strength <= 1)) {
      throw new IllegalArgumentException("strength must be in [0, 1]: " + strength);
    }
    this.blockX = blockX;
    this.blockY = blockY;
    this.height = height;
    this.colour =
        new Color(
            SUNLIGHT_COLOUR.getRed(),
            SUNLIGHT_COLOUR.getGreen(),
            SUNLIGHT_COLOUR.getBlue(),
            (int) Math.round(strength * SUNLIGHT_COLOUR.getAlpha()));
  }

  /** Returns {@code null}: the overlay is a plain fill, not an image file. */
  @Override
  public String imageFolder() {
    return null;
  }

  /** Fills the tile's top, with its top-left corner at {@code (x, y)}. */
  @Override
  public void paintOnto(Graphics2D g, int x, int y, TerrainImages images) {
    g.setColor(colour);
    g.fillRect(x, y, BLOCK, BLOCK);
  }

  /** Returns a texture showing the overlay, painting it the first time. */
  @Override
  public Texture getTexture() {
    if (texture == null) {
      BufferedImage image = new BufferedImage(BLOCK, BLOCK, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = image.createGraphics();
      try {
        paintOnto(g, 0, 0, null);
      } finally {
        g.dispose();
      }
      texture = new ImageTexture(image);
    }
    return texture;
  }

  /** Returns the tile's centre, so the overlay lines up with it. */
  @Override
  public int getX() {
    return TerrainCoordinates.blockToPixel(blockX) + BLOCK / 2;
  }

  /** Returns the bottom edge of the tile's top, so the overlay covers exactly the top. */
  @Override
  public int getY() {
    return TerrainCoordinates.blockToPixel(blockY) + height * TileRenderable.STEP_HEIGHT_PX;
  }

  @Override
  public int getWidth() {
    return BLOCK;
  }

  @Override
  public int getHeight() {
    return BLOCK;
  }
}
