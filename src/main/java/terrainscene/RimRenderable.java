package terrainscene;

import game.gameModel.Renderable;
import java.util.Locale;
import jswing.Texture;
import terrain.BlockType;
import terrain.TerrainCoordinates;

/**
 * A dark line traced along one edge of a tile's top, where the neighbouring tile on that side is
 * lower.
 *
 * <p>Rims make every drop-off visible, including edges that face away from the camera or toward
 * the sun, which show no face and no shadow. The line is a darker shade of the tile's own block,
 * such as dark green on grass.
 */
public final class RimRenderable implements TerrainPiece {

  /** How thick a rim line is, in pixels. */
  public static final int THICKNESS_PX = 3;

  private static final long serialVersionUID = 1L;

  /** Image folder, under the textures root, holding rim images by block type and side. */
  static final String TEXTURE_FOLDER = TileRenderable.TEXTURE_FOLDER + "rim/";

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  private final int blockX;
  private final int blockY;
  private final int height;
  private final Edge side;

  private final String imageFolder;

  /** Loaded from its image on first use, and never saved: pieces are rebuilt from terrain. */
  private transient Texture texture;

  /**
   * Creates a rim.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @param type the tile's block type, which picks the rim colour
   * @param height the tile's terrace height, so the rim sits on its top
   * @param side which edge of the tile the rim runs along
   */
  public RimRenderable(int blockX, int blockY, BlockType type, int height, Edge side) {
    this.blockX = blockX;
    this.blockY = blockY;
    this.height = height;
    this.side = side;
    String sideFolder = side.name().toLowerCase(Locale.ROOT);
    this.imageFolder = TEXTURE_FOLDER + type.getLabel() + "/" + sideFolder;
  }

  /**
   * Returns which edge this rim runs along.
   *
   * @return the rim's side
   */
  public Edge getSide() {
    return side;
  }

  @Override
  public String imageFolder() {
    return imageFolder;
  }

  /** Returns this piece's texture, loading its image the first time it is needed. */
  @Override
  public Texture getTexture() {
    if (texture == null) {
      texture = new Texture(imageFolder);
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
