package terrainscene;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import jswing.Texture;
import terrain.TerrainCoordinates;

/**
 * A shadow lying on a tile's top, cast by taller terrain toward the sun.
 *
 * <p>Unlike most pieces it is a shape rather than an image file: a see-through fill of the shaded
 * region worked out by {@link SunShadows}, so the tile underneath still shows through. The fill
 * is a cool blue rather than black, which makes the sunlit ground around it look warmer, as
 * shadows outdoors do. A shadow made of several regions (from {@link SunShadows#softShadowOn})
 * fills each one faintly, so its edges fade out. Near dawn and dusk the whole shadow is fainter,
 * because sunlight is weak.
 */
public final class ShadowRenderable implements TerrainPiece {

  /** The colour where the shadow is darkest: a deep blue at about 40% opacity. */
  static final Color SHADOW_COLOUR = new Color(24, 32, 96, 104);

  private static final long serialVersionUID = 1L;

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  private final int blockX;
  private final int blockY;
  private final int height;
  private final double strength;

  /** Kept only while the scene is live; shadows are recomputed from the terrain, never saved. */
  private final transient List<Area> layers;

  /** Painted from the layers on first use, and never saved. */
  private transient BufferedImage image;

  /** Made from the image on first use, and never saved. */
  private transient Texture texture;

  /**
   * Creates a shadow with sharp edges.
   *
   * @param blockX the x coordinate of the shaded tile, in blocks
   * @param blockY the y coordinate of the shaded tile, in blocks
   * @param height the shaded tile's terrace height, so the shadow sits on its top
   * @param shape the shaded region in the tile's own coordinates (y up), as from {@link
   *     SunShadows#shadowOn(int, int)}
   */
  public ShadowRenderable(int blockX, int blockY, int height, Area shape) {
    this(blockX, blockY, height, List.of(shape), 1);
  }

  /**
   * Creates a shadow from several overlapping regions, darkest where they all overlap.
   *
   * @param blockX the x coordinate of the shaded tile, in blocks
   * @param blockY the y coordinate of the shaded tile, in blocks
   * @param height the shaded tile's terrace height, so the shadow sits on its top
   * @param layers the regions in the tile's own coordinates (y up), as from {@link
   *     SunShadows#softShadowOn(int, int, ShadowCast)}; at least one
   * @param strength how strong sunlight is, from 0 to 1; the shadow is this fraction as dark
   * @throws IllegalArgumentException if {@code layers} is empty or {@code strength} is out of
   *     range
   */
  public ShadowRenderable(
      int blockX, int blockY, int height, List<Area> layers, double strength) {
    if (layers.isEmpty()) {
      throw new IllegalArgumentException("a shadow needs at least one region");
    }
    if (!(strength >= 0 && strength <= 1)) {
      throw new IllegalArgumentException("strength must be in [0, 1]: " + strength);
    }
    this.blockX = blockX;
    this.blockY = blockY;
    this.height = height;
    this.strength = strength;
    this.layers = new ArrayList<>();
    for (Area layer : layers) {
      this.layers.add(new Area(layer));
    }
  }

  /**
   * Returns the shaded region: everywhere any part of the shadow reaches.
   *
   * @return a copy of the region, in the tile's own coordinates with y up
   */
  public Area getShape() {
    Area shape = new Area();
    for (Area layer : layers) {
      shape.add(layer);
    }
    return shape;
  }

  /** Returns {@code null}: a shadow is drawn as a shape, not from an image file. */
  @Override
  public String imageFolder() {
    return null;
  }

  /** Draws the shadow with the tile's top-left corner at {@code (x, y)}. */
  @Override
  public void paintOnto(Graphics2D g, int x, int y, TerrainImages images) {
    g.drawImage(image(), x, y, null);
  }

  /** Returns a texture showing the shadow. */
  @Override
  public Texture getTexture() {
    if (texture == null) {
      texture = new ImageTexture(image());
    }
    return texture;
  }

  /**
   * Returns the shadow painted into a tile-sized image, painting it the first time.
   *
   * <p>Both the renderer and {@link ChunkPainter} draw this same image, so a shadow looks the same
   * either way. Each layer is filled faintly enough that all of them together reach {@link
   * #SHADOW_COLOUR}'s opacity, scaled by the sunlight's strength.
   */
  private synchronized BufferedImage image() {
    if (image == null) {
      BufferedImage painted = new BufferedImage(BLOCK, BLOCK, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = painted.createGraphics();
      try {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double fullOpacity = strength * SHADOW_COLOUR.getAlpha() / 255.0;
        double layerOpacity = 1 - Math.pow(1 - fullOpacity, 1.0 / layers.size());
        g.setColor(
            new Color(
                SHADOW_COLOUR.getRed(),
                SHADOW_COLOUR.getGreen(),
                SHADOW_COLOUR.getBlue(),
                (int) Math.round(layerOpacity * 255)));
        // Tile coordinates have y up; images have y down, so flip about the tile's height.
        AffineTransform tileToImage = new AffineTransform(1, 0, 0, -1, 0, BLOCK);
        for (Area layer : layers) {
          g.fill(tileToImage.createTransformedShape(layer));
        }
      } finally {
        g.dispose();
      }
      image = painted;
    }
    return image;
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
