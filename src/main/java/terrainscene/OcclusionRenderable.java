package terrainscene;

import game.gameModel.Renderable;
import java.util.Locale;
import jswing.Texture;
import terrain.TerrainCoordinates;

/**
 * Ambient occlusion at the foot of a wall: a soft dark gradient on a tile's top along an edge
 * where it meets higher ground, darkest against the wall and fading smoothly toward the middle.
 * Where two walls meet, the two gradients overlap and the corner is darker still; {@link
 * CornerOcclusionRenderable} covers corners where only a diagonal neighbour is higher.
 *
 * <p>It does not depend on the sun, so it shows which side is higher whatever the light. Only the
 * back, left and right edges get it; a higher tile in front would cover the front edge anyway.
 */
public final class OcclusionRenderable implements TerrainPiece {

  /** How far the gradient reaches into the tile from the wall, in pixels. */
  public static final int THICKNESS_PX = 20;

  private static final long serialVersionUID = 1L;

  /** Image folder, under the textures root, holding one gradient image per edge. */
  static final String TEXTURE_FOLDER = TileRenderable.TEXTURE_FOLDER + "occlusion/";

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  private final int blockX;
  private final int blockY;
  private final int height;
  private final Edge edge;

  private final String imageFolder;

  /** Loaded from its image on first use, and never saved: pieces are rebuilt from terrain. */
  private transient Texture texture;

  /**
   * Creates the gradient for one edge of a tile.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @param height the tile's terrace height, so the gradient sits on its top
   * @param edge which edge meets the higher ground: {@code BACK}, {@code LEFT} or {@code RIGHT}
   * @throws IllegalArgumentException if {@code edge} is {@code FRONT}
   */
  public OcclusionRenderable(int blockX, int blockY, int height, Edge edge) {
    if (edge == Edge.FRONT) {
      throw new IllegalArgumentException("front edges are never occluded");
    }
    this.blockX = blockX;
    this.blockY = blockY;
    this.height = height;
    this.edge = edge;
    this.imageFolder = TEXTURE_FOLDER + edge.name().toLowerCase(Locale.ROOT);
  }

  /**
   * Returns which edge of the tile this gradient runs along.
   *
   * @return the edge against the higher ground
   */
  public Edge getEdge() {
    return edge;
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

  /** Returns the gradient's centre: mid-tile for the back edge, else hugging that side. */
  @Override
  public int getX() {
    int tileLeft = TerrainCoordinates.blockToPixel(blockX);
    switch (edge) {
      case LEFT:
        return tileLeft + THICKNESS_PX / 2;
      case RIGHT:
        return tileLeft + BLOCK - THICKNESS_PX + THICKNESS_PX / 2;
      case BACK:
      default:
        return tileLeft + BLOCK / 2;
    }
  }

  /** Returns the gradient's bottom edge: just under the top edge for the back, else the base. */
  @Override
  public int getY() {
    int tileBottom =
        TerrainCoordinates.blockToPixel(blockY) + height * TileRenderable.STEP_HEIGHT_PX;
    return edge == Edge.BACK ? tileBottom + BLOCK - THICKNESS_PX : tileBottom;
  }

  @Override
  public int getWidth() {
    return edge == Edge.BACK ? BLOCK : THICKNESS_PX;
  }

  @Override
  public int getHeight() {
    return edge == Edge.BACK ? THICKNESS_PX : BLOCK;
  }
}
