package terrainscene;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import jswing.Texture;
import terrain.TerrainCoordinates;

/**
 * A shadow lying on a tile's top, cast by taller terrain toward the sun.
 *
 * <p>Unlike other pieces it is a shape rather than an image file: a dark, see-through fill of the
 * shaded region worked out by {@link SunShadows}, so the tile underneath still shows through.
 */
public final class ShadowRenderable implements TerrainPiece {

  /** The shadow's colour: black at about 35% opacity. */
  static final Color SHADOW_COLOUR = new Color(0, 0, 0, 90);

  private static final long serialVersionUID = 1L;

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  private final int blockX;
  private final int blockY;
  private final int height;

  /** Kept only while the scene is live; shadows are recomputed from the terrain, never saved. */
  private final transient Area shape;

  /** Made from the shape on first use, and never saved. */
  private transient Texture texture;

  /**
   * Creates a shadow.
   *
   * @param blockX the x coordinate of the shaded tile, in blocks
   * @param blockY the y coordinate of the shaded tile, in blocks
   * @param height the shaded tile's terrace height, so the shadow sits on its top
   * @param shape the shaded region in the tile's own coordinates (y up), as from {@link
   *     SunShadows#shadowOn(int, int)}
   */
  public ShadowRenderable(int blockX, int blockY, int height, Area shape) {
    this.blockX = blockX;
    this.blockY = blockY;
    this.height = height;
    this.shape = new Area(shape);
  }

  /**
   * Returns the shaded region.
   *
   * @return a copy of the region, in the tile's own coordinates with y up
   */
  public Area getShape() {
    return new Area(shape);
  }

  /** Returns {@code null}: a shadow is drawn as a shape, not from an image file. */
  @Override
  public String imageFolder() {
    return null;
  }

  /** Fills the shadow's shape, with the tile's top-left corner at {@code (x, y)}. */
  @Override
  public void paintOnto(Graphics2D g, int x, int y, TerrainImages images) {
    // Tile coordinates have y up; images have y down, so flip about the tile's height.
    AffineTransform tileToImage = new AffineTransform(1, 0, 0, -1, x, y + BLOCK);
    g.setColor(SHADOW_COLOUR);
    g.fill(tileToImage.createTransformedShape(shape));
  }

  /** Returns a texture showing the shadow, drawing it into an image the first time. */
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

  /** Returns the tile's centre: the shadow covers the whole tile, mostly see-through. */
  @Override
  public int getX() {
    return TerrainCoordinates.blockToPixel(blockX) + BLOCK / 2;
  }

  /** Returns the bottom edge of the shaded tile's top, so the shadow lies on it. */
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
