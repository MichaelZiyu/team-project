package terrainscene;

import game.gameModel.Renderable;
import jswing.Texture;
import terrain.TerrainCoordinates;

/**
 * A see-through cool overlay on one face step, so the sides of terraces, which face away from the
 * sun, look darker and bluer than the sunlit tops.
 *
 * <p>Because it is an overlay rather than a darker copy of the face image, it works the same with
 * placeholder images and with real textures. The colour (deep blue at about 30%) lives in its
 * image, {@code terrain/faceshade/000-1.png}.
 */
public final class FaceShadeRenderable implements TerrainPiece {

  private static final long serialVersionUID = 1L;

  /** Image folder, under the textures root, holding the overlay. */
  static final String TEXTURE_FOLDER = TileRenderable.TEXTURE_FOLDER + "faceshade";

  private final int blockX;
  private final int blockY;
  private final int level;

  private final String imageFolder;

  /** Loaded from its image on first use, and never saved: pieces are rebuilt from terrain. */
  private transient Texture texture;

  /**
   * Creates the overlay for one face step.
   *
   * @param blockX the x coordinate of the tile the face belongs to, in blocks
   * @param blockY the y coordinate of the tile the face belongs to, in blocks
   * @param level which step the overlay covers, matching {@link FaceRenderable#getLevel()}
   */
  public FaceShadeRenderable(int blockX, int blockY, int level) {
    this.blockX = blockX;
    this.blockY = blockY;
    this.level = level;
    this.imageFolder = TEXTURE_FOLDER;
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

  /** Returns the face's centre, so the overlay lines up with it. */
  @Override
  public int getX() {
    return TerrainCoordinates.blockToPixel(blockX) + TerrainCoordinates.BLOCK_SIZE_PX / 2;
  }

  /** Returns the face step's bottom edge, so the overlay covers exactly that step. */
  @Override
  public int getY() {
    return TerrainCoordinates.blockToPixel(blockY) + level * TileRenderable.STEP_HEIGHT_PX;
  }

  @Override
  public int getWidth() {
    return TerrainCoordinates.BLOCK_SIZE_PX;
  }

  @Override
  public int getHeight() {
    return TileRenderable.STEP_HEIGHT_PX;
  }
}
