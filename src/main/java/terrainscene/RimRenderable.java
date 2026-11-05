package terrainscene;

import game.gameModel.Renderable;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Locale;
import jswing.Texture;
import terrain.BlockType;
import terrain.TerrainCoordinates;

/**
 * A line traced along one edge of a tile's top, where the neighbouring tile on that side is lower.
 *
 * <p>Rims make every drop-off visible, including edges that face away from the camera or toward
 * the sun, which show no face and no shadow. Edges facing the sun catch its light, so their rim
 * is a bright, warm shade of the tile's own block, such as light green on grass; the other edges
 * get a darker shade, such as dark green. Together they make each terrace step look bevelled.
 * How brightly each edge is lit follows the sun through the day ({@link ShadowCast#edgeLight}),
 * blending smoothly between the dark and bright images, so rims brighten and darken gradually
 * rather than switching; at night all rims are dark.
 */
public final class RimRenderable implements TerrainPiece {

  /** How thick a rim line is, in pixels. */
  public static final int THICKNESS_PX = 3;

  private static final long serialVersionUID = 1L;

  /** Image folder, under the textures root, holding dark rim images by block type and side. */
  static final String TEXTURE_FOLDER = TileRenderable.TEXTURE_FOLDER + "rim/";

  /** Image folder, under the textures root, holding sunlit rim images by block type and side. */
  static final String LIT_TEXTURE_FOLDER = TileRenderable.TEXTURE_FOLDER + "rimlit/";

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  /** Loads both rim images when a part-lit rim is shown piece by piece rather than in a chunk. */
  private static final TerrainImages RIM_IMAGES = new TerrainImages();

  private final int blockX;
  private final int blockY;
  private final int height;
  private final Edge side;
  private final double light;

  private final String imageFolder;
  private final String litImageFolder;

  /** Loaded from its images on first use, and never saved: pieces are rebuilt from terrain. */
  private transient Texture texture;

  /**
   * Creates a rim.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @param type the tile's block type, which picks the rim colour
   * @param height the tile's terrace height, so the rim sits on its top
   * @param side which edge of the tile the rim runs along
   * @param light how much sunlight the edge catches, from 0 (a dark rim) to 1 (a bright one)
   * @throws IllegalArgumentException if {@code light} is out of range
   */
  public RimRenderable(
      int blockX, int blockY, BlockType type, int height, Edge side, double light) {
    if (!(light >= 0 && light <= 1)) {
      throw new IllegalArgumentException("light must be in [0, 1]: " + light);
    }
    this.blockX = blockX;
    this.blockY = blockY;
    this.height = height;
    this.side = side;
    this.light = light;
    String path = type.getLabel() + "/" + side.name().toLowerCase(Locale.ROOT);
    this.imageFolder = TEXTURE_FOLDER + path;
    this.litImageFolder = LIT_TEXTURE_FOLDER + path;
  }

  /**
   * Returns which edge this rim runs along.
   *
   * @return the rim's side
   */
  public Edge getSide() {
    return side;
  }

  /**
   * Returns how much sunlight this rim catches.
   *
   * @return from 0 (fully dark) to 1 (fully bright)
   */
  public double getLight() {
    return light;
  }

  /** Returns the folder of the dark image, which the bright one is blended over. */
  @Override
  public String imageFolder() {
    return imageFolder;
  }

  /** Paints the dark image, then the bright image over it as strongly as the rim is lit. */
  @Override
  public void paintOnto(Graphics2D g, int x, int y, TerrainImages images) {
    g.drawImage(images.get(imageFolder), x, y, getWidth(), getHeight(), null);
    if (light > 0) {
      Composite before = g.getComposite();
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) light));
      g.drawImage(images.get(litImageFolder), x, y, getWidth(), getHeight(), null);
      g.setComposite(before);
    }
  }

  /** Returns this piece's texture, loading its images the first time it is needed. */
  @Override
  public Texture getTexture() {
    if (texture == null) {
      if (light == 0) {
        texture = new Texture(imageFolder);
      } else {
        BufferedImage image =
            new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
          paintOnto(g, 0, 0, RIM_IMAGES);
        } finally {
          g.dispose();
        }
        texture = new ImageTexture(image);
      }
    }
    return texture;
  }

  /** Returns the line's centre: mid-tile for the back and front edges, else at that edge. */
  @Override
  public int getX() {
    int tileLeft = TerrainCoordinates.blockToPixel(blockX);
    switch (side) {
      case LEFT:
        return tileLeft + THICKNESS_PX / 2;
      case RIGHT:
        return tileLeft + BLOCK - THICKNESS_PX + THICKNESS_PX / 2;
      case BACK:
      case FRONT:
      default:
        return tileLeft + BLOCK / 2;
    }
  }

  /** Returns the line's bottom edge: just under the top edge for a back rim, else the base. */
  @Override
  public int getY() {
    int tileBottom =
        TerrainCoordinates.blockToPixel(blockY) + height * TileRenderable.STEP_HEIGHT_PX;
    return side == Edge.BACK ? tileBottom + BLOCK - THICKNESS_PX : tileBottom;
  }

  @Override
  public int getWidth() {
    return isHorizontal() ? BLOCK : THICKNESS_PX;
  }

  @Override
  public int getHeight() {
    return isHorizontal() ? THICKNESS_PX : BLOCK;
  }

  /** Returns whether this rim runs across the tile (back or front) rather than down it. */
  private boolean isHorizontal() {
    return side == Edge.BACK || side == Edge.FRONT;
  }
}
