package terrainscene;

import game.gameModel.Renderable;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jswing.Texture;
import terrain.BlockType;
import terrain.TerrainCoordinates;

/**
 * One step of a terrace's front face: the strip of wall visible below a raised tile.
 *
 * <p>A tile that stands several levels higher than the tile in front of it shows one face per
 * level, stacked, so cliffs look as tall as they are. The top step uses an image chosen by the
 * tile's block type, such as a grassy lip under grass. The steps below it are plain earth, unless
 * the block type has a face image of its own (a folder named after it under {@link
 * #TEXTURE_FOLDER}), as sand and snow do, being sand or snow all the way down.
 */
public final class FaceRenderable implements TerrainPiece {

  private static final long serialVersionUID = 1L;

  /**
   * Image folder, under the textures root, for the plain face steps; a block type with lower steps
   * of its own has a folder named after it inside.
   */
  static final String TEXTURE_FOLDER = TileRenderable.TEXTURE_FOLDER + "face";

  /** Each block type's folder for its lower steps, looked up once. */
  private static final Map<BlockType, String> LOWER_STEP_FOLDERS = new ConcurrentHashMap<>();

  /** Image folder, under the textures root, holding one top-step image folder per block type. */
  static final String TOP_STEP_TEXTURE_FOLDER = TileRenderable.TEXTURE_FOLDER + "facetop/";

  private final int blockX;
  private final int blockY;
  private final int level;
  private final boolean topStep;

  private final String imageFolder;

  /** Loaded from its image on first use, and never saved: pieces are rebuilt from terrain. */
  private transient Texture texture;

  /**
   * Creates one face step.
   *
   * @param blockX the x coordinate of the tile this face belongs to, in blocks
   * @param blockY the y coordinate of the tile this face belongs to, in blocks
   * @param tileType the block type on top of that tile, which picks the top step's image
   * @param level which step this is; the face spans from height {@code level} to {@code level + 1}
   * @param topStep whether this is the highest step, directly under the tile
   */
  public FaceRenderable(int blockX, int blockY, BlockType tileType, int level, boolean topStep) {
    this.blockX = blockX;
    this.blockY = blockY;
    this.level = level;
    this.topStep = topStep;
    this.imageFolder =
        topStep ? TOP_STEP_TEXTURE_FOLDER + tileType.getLabel() : lowerStepFolder(tileType);
  }

  /**
   * Returns the image folder for a block type's lower face steps: its own, if it has one, or else
   * the plain face.
   *
   * @param tileType the block type on top of the tile
   * @return an image folder, under the textures root
   */
  static String lowerStepFolder(BlockType tileType) {
    return LOWER_STEP_FOLDERS.computeIfAbsent(
        tileType,
        type -> {
          String own = TEXTURE_FOLDER + "/" + type.getLabel();
          return Files.isDirectory(TerrainImages.TEXTURES_ROOT.resolve(own))
              ? own
              : TEXTURE_FOLDER;
        });
  }

  /**
   * Returns which step of the face this is.
   *
   * @return the height level where this strip starts
   */
  public int getLevel() {
    return level;
  }

  /**
   * Returns whether this is the highest step, directly under the tile.
   *
   * @return true for the top step
   */
  public boolean isTopStep() {
    return topStep;
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

  /** Returns the face's centre, matching the tile above it. */
  @Override
  public int getX() {
    return TerrainCoordinates.blockToPixel(blockX) + TerrainCoordinates.BLOCK_SIZE_PX / 2;
  }

  /** Returns the face's bottom edge: the tile's base lifted to this step's level. */
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
