package terrainscene;

import jswing.Texture;
import terrain.TerrainCoordinates;

/**
 * A flower growing on a grass tile: an image lying on the tile's top, drawn over the grass and
 * under the shadows and light that fall on the tile, so a flower in shade is shaded too.
 */
public final class FlowerRenderable implements TerrainPiece {

  private static final long serialVersionUID = 1L;

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  private final int blockX;
  private final int blockY;
  private final int height;
  private final String name;

  /** Loaded from its image on first use, and never saved: pieces are rebuilt from terrain. */
  private transient Texture texture;

  /**
   * Creates a flower.
   *
   * @param blockX the x coordinate of its tile, in blocks
   * @param blockY the y coordinate of its tile, in blocks
   * @param height its tile's terrace height, so it lies on the tile's top
   * @param name which flower: the name of its image folder under {@link Flowers#TEXTURE_FOLDER}
   */
  public FlowerRenderable(int blockX, int blockY, int height, String name) {
    this.blockX = blockX;
    this.blockY = blockY;
    this.height = height;
    this.name = name;
  }

  /**
   * Returns which flower this is.
   *
   * @return its name
   */
  public String getName() {
    return name;
  }

  @Override
  public String imageFolder() {
    return Flowers.TEXTURE_FOLDER + name;
  }

  @Override
  public Texture getTexture() {
    if (texture == null) {
      texture = new Texture(imageFolder());
    }
    return texture;
  }

  /** Returns the tile's centre. */
  @Override
  public int getX() {
    return TerrainCoordinates.blockToPixel(blockX) + BLOCK / 2;
  }

  /** Returns the bottom edge of the tile's top, lifted by its height. */
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
