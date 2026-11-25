package terrainscene;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import jswing.Texture;
import terrain.TerrainCoordinates;

/**
 * A see-through colour laid over part of a tile: its whole top, or one step of its face. Used for
 * light that changes how bright the terrain is, such as the glare a lightning strike throws on
 * tile tops and the deep shade it leaves on terrace faces.
 */
public final class TintRenderable implements TerrainPiece {

  /** The colour a lightning strike's glare adds to tile tops, before its opacity. */
  static final Color GLOW_COLOUR = new Color(255, 255, 255);

  /** The colour a lightning strike's shade adds to terrace faces, before its opacity. */
  static final Color DARK_COLOUR = new Color(6, 8, 20);

  private static final long serialVersionUID = 1L;

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  private final int centreX;
  private final int bottomY;
  private final int height;
  private final Color colour;

  /** Made on first use, and never saved: pieces are rebuilt from terrain. */
  private transient Texture texture;

  private TintRenderable(int centreX, int bottomY, int height, Color colour) {
    this.centreX = centreX;
    this.bottomY = bottomY;
    this.height = height;
    this.colour = colour;
  }

  /**
   * Creates a glow over a tile's top.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @param tileHeight the tile's terrace height, so the glow sits on its top
   * @param amount how strong the glow is, from 0 (none) to 1 (white)
   * @return the glow
   * @throws IllegalArgumentException if {@code amount} is out of range
   */
  public static TintRenderable topGlow(int blockX, int blockY, int tileHeight, double amount) {
    return new TintRenderable(
        TerrainCoordinates.blockToPixel(blockX) + BLOCK / 2,
        TerrainCoordinates.blockToPixel(blockY) + tileHeight * TileRenderable.STEP_HEIGHT_PX,
        BLOCK,
        withOpacity(GLOW_COLOUR, amount));
  }

  /**
   * Creates a darkening over one step of a tile's face.
   *
   * @param blockX the x coordinate of the tile the face belongs to, in blocks
   * @param blockY the y coordinate of the tile the face belongs to, in blocks
   * @param level which step it covers, matching {@link FaceRenderable#getLevel()}
   * @param amount how dark it is, from 0 (not at all) to 1 (black)
   * @return the darkening
   * @throws IllegalArgumentException if {@code amount} is out of range
   */
  public static TintRenderable faceDarkening(int blockX, int blockY, int level, double amount) {
    return new TintRenderable(
        TerrainCoordinates.blockToPixel(blockX) + BLOCK / 2,
        TerrainCoordinates.blockToPixel(blockY) + level * TileRenderable.STEP_HEIGHT_PX,
        TileRenderable.STEP_HEIGHT_PX,
        withOpacity(DARK_COLOUR, amount));
  }

  /**
   * Returns the colour laid over the tile.
   *
   * @return a see-through colour
   */
  public Color getColour() {
    return colour;
  }

  /** Returns {@code null}: a tint is a plain fill, not an image file. */
  @Override
  public String imageFolder() {
    return null;
  }

  /** Fills the covered part of the tile, with its top-left corner at {@code (x, y)}. */
  @Override
  public void paintOnto(Graphics2D g, int x, int y, TerrainImages images) {
    g.setColor(colour);
    g.fillRect(x, y, BLOCK, height);
  }

  /** Returns a texture showing the tint, painting it the first time. */
  @Override
  public Texture getTexture() {
    if (texture == null) {
      BufferedImage image = new BufferedImage(BLOCK, height, BufferedImage.TYPE_INT_ARGB);
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

  @Override
  public int getX() {
    return centreX;
  }

  @Override
  public int getY() {
    return bottomY;
  }

  @Override
  public int getWidth() {
    return BLOCK;
  }

  @Override
  public int getHeight() {
    return height;
  }

  private static Color withOpacity(Color colour, double amount) {
    if (!(amount >= 0 && amount <= 1)) {
      throw new IllegalArgumentException("amount must be in [0, 1]: " + amount);
    }
    return new Color(
        colour.getRed(), colour.getGreen(), colour.getBlue(), (int) Math.round(255 * amount));
  }
}
