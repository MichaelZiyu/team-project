package terrainscene;

import jswing.Texture;
import terrain.TerrainCoordinates;

/**
 * A see-through warm overlay on a tile's top, so ground in the sun looks golden next to the cool
 * blue of shadows and terrace faces.
 *
 * <p>Every tile top gets one, under its shadow, so shaded parts end up cool and the rest warm.
 * Like {@link FaceShadeRenderable} it is an overlay rather than a tinted copy of the tile image,
 * so it works the same with placeholder images and with real textures. The colour lives in its
 * image, {@code terrain/sunlight/000-1.png}.
 */
public final class SunlightRenderable implements TerrainPiece {

  private static final long serialVersionUID = 1L;

  /** Image folder, under the textures root, holding the overlay. */
  static final String TEXTURE_FOLDER = TileRenderable.TEXTURE_FOLDER + "sunlight";

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  private final int blockX;
  private final int blockY;
  private final int height;

  /** Loaded from its image on first use, and never saved: pieces are rebuilt from terrain. */
  private transient Texture texture;

  /**
   * Creates the overlay for one tile's top.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @param height the tile's terrace height, so the overlay sits on its top
   */
  public SunlightRenderable(int blockX, int blockY, int height) {
    this.blockX = blockX;
    this.blockY = blockY;
    this.height = height;
  }

  @Override
  public String imageFolder() {
    return TEXTURE_FOLDER;
  }

  /** Returns this piece's texture, loading its image the first time it is needed. */
  @Override
  public Texture getTexture() {
    if (texture == null) {
      texture = new Texture(TEXTURE_FOLDER);
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
