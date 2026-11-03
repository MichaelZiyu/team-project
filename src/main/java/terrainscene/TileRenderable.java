package terrainscene;

import game.gameModel.Renderable;
import jswing.Texture;
import terrain.BlockType;
import terrain.TerrainCoordinates;

/**
 * The top surface of one terrain tile, adapted so the game's renderer can draw it like any
 * other sprite.
 *
 * <p>This is the Adapter pattern: it wraps a block from the {@code terrain} package in the
 * renderer's {@link Renderable} interface, so the terrain package itself never depends on Swing.
 * Raised tiles are drawn shifted up by {@link #STEP_HEIGHT_PX} per height level, which makes
 * hills look terraced from the top-down camera.
 */
public final class TileRenderable implements TerrainPiece {

  /** How far each height level lifts a tile on screen, in pixels. */
  public static final int STEP_HEIGHT_PX = 32;

  private static final long serialVersionUID = 1L;

  /** Folder, under the textures root, holding one placeholder image folder per block type. */
  static final String TEXTURE_FOLDER = "topdown/terrain/";

  private final int blockX;
  private final int blockY;
  private final BlockType type;
  private final int height;

  private final String imageFolder;

  /** Loaded from its image on first use, and never saved: pieces are rebuilt from terrain. */
  private transient Texture texture;

  /**
   * Creates a tile.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @param type the block type shown on the tile
   * @param height the tile's terrace height; 0 for water
   */
  public TileRenderable(int blockX, int blockY, BlockType type, int height) {
    this.blockX = blockX;
    this.blockY = blockY;
    this.type = type;
    this.height = height;
    this.imageFolder = TEXTURE_FOLDER + type.getLabel();
  }

  /**
   * Returns the tile's x coordinate.
   *
   * @return the x coordinate, in blocks
   */
  public int getBlockX() {
    return blockX;
  }

  /**
   * Returns the tile's y coordinate.
   *
   * @return the y coordinate, in blocks
   */
  public int getBlockY() {
    return blockY;
  }

  /**
   * Returns the block type shown on this tile.
   *
   * @return the tile's block type
   */
  public BlockType getType() {
    return type;
  }

  /**
   * Returns the tile's terrace height.
   *
   * @return the height in blocks; 0 for water
   */
  public int getTerrainHeight() {
    return height;
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

  /** Returns the tile's centre, since the renderer's camera positions sprites by their centre. */
  @Override
  public int getX() {
    return TerrainCoordinates.blockToPixel(blockX) + TerrainCoordinates.BLOCK_SIZE_PX / 2;
  }

  /** Returns the tile's bottom edge, lifted by its height so hills appear raised. */
  @Override
  public int getY() {
    return TerrainCoordinates.blockToPixel(blockY) + height * STEP_HEIGHT_PX;
  }

  @Override
  public int getWidth() {
    return TerrainCoordinates.BLOCK_SIZE_PX;
  }

  @Override
  public int getHeight() {
    return TerrainCoordinates.BLOCK_SIZE_PX;
  }
}
