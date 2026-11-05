package terrainscene;

import java.util.Locale;
import jswing.Texture;
import terrain.TerrainCoordinates;

/**
 * Ambient occlusion in a tile's back corner, where only the diagonal neighbour is higher: a small
 * soft shadow fanning out from the corner that the higher tile touches.
 *
 * <p>Fills the gap {@link OcclusionRenderable} leaves, since its strips run along whole edges and
 * appear only when a side neighbour is higher. Front corners do not need one: a higher tile in
 * front covers that part of the tile anyway.
 */
public final class CornerOcclusionRenderable implements TerrainPiece {

  /** How far the shadow reaches from the corner, in pixels, matching the edge strips. */
  public static final int SIZE_PX = OcclusionRenderable.THICKNESS_PX;

  private static final long serialVersionUID = 1L;

  /** Image folder, under the textures root, holding one image per corner. */
  static final String TEXTURE_FOLDER = OcclusionRenderable.TEXTURE_FOLDER + "corner/";

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  private final int blockX;
  private final int blockY;
  private final int height;
  private final Corner corner;

  private final String imageFolder;

  /** Loaded from its image on first use, and never saved: pieces are rebuilt from terrain. */
  private transient Texture texture;

  /**
   * Creates the shadow for one back corner of a tile.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @param height the tile's terrace height, so the shadow sits on its top
   * @param corner which back corner touches the higher tile
   */
  public CornerOcclusionRenderable(int blockX, int blockY, int height, Corner corner) {
    this.blockX = blockX;
    this.blockY = blockY;
    this.height = height;
    this.corner = corner;
    this.imageFolder = TEXTURE_FOLDER + corner.name().toLowerCase(Locale.ROOT).replace("_", "");
  }

  /**
   * Returns which corner of the tile this shadow sits in.
   *
   * @return the corner touching the higher tile
   */
  public Corner getCorner() {
    return corner;
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

  /** Returns the shadow's centre, hugging the left or right side of the tile. */
  @Override
  public int getX() {
    int tileLeft = TerrainCoordinates.blockToPixel(blockX);
    int left = corner == Corner.BACK_LEFT ? tileLeft : tileLeft + BLOCK - SIZE_PX;
    return left + SIZE_PX / 2;
  }

  /** Returns the shadow's bottom edge, just under the tile's top edge. */
  @Override
  public int getY() {
    int tileBottom =
        TerrainCoordinates.blockToPixel(blockY) + height * TileRenderable.STEP_HEIGHT_PX;
    return tileBottom + BLOCK - SIZE_PX;
  }

  @Override
  public int getWidth() {
    return SIZE_PX;
  }

  @Override
  public int getHeight() {
    return SIZE_PX;
  }
}
