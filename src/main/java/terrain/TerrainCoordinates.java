package terrain;

/**
 * Converts between pixel, block and chunk coordinates.
 *
 * <p>The game positions things in pixels, while terrain works in whole blocks. A block is
 * {@value #BLOCK_SIZE_PX} pixels wide and a chunk is {@value #CHUNK_SIZE} blocks wide. All
 * conversions round toward negative infinity, so coordinates left of or below the origin map to
 * the correct block and chunk.
 */
public final class TerrainCoordinates {

  /** The width and height of one block, in pixels. */
  public static final int BLOCK_SIZE_PX = 64;

  /** The width and height of one chunk, in blocks. */
  public static final int CHUNK_SIZE = 16;

  private TerrainCoordinates() {}

  /**
   * Returns the block that contains a pixel coordinate.
   *
   * @param px a pixel coordinate on either axis; may be negative
   * @return the block coordinate on the same axis, for example {@code -1} for {@code px = -0.5}
   */
  public static int pixelToBlock(double px) {
    return (int) Math.floor(px / BLOCK_SIZE_PX);
  }

  /**
   * Returns the pixel coordinate of a block's lower edge.
   *
   * @param block a block coordinate on either axis; may be negative
   * @return the pixel coordinate where that block starts
   */
  public static int blockToPixel(int block) {
    return block * BLOCK_SIZE_PX;
  }

  /**
   * Returns the chunk that contains a block coordinate.
   *
   * @param block a block coordinate on either axis; may be negative
   * @return the chunk coordinate on the same axis, for example {@code -1} for {@code block = -1}
   */
  public static int blockToChunk(int block) {
    return Math.floorDiv(block, CHUNK_SIZE);
  }

  /**
   * Returns a block's position inside its chunk.
   *
   * @param block a block coordinate on either axis; may be negative
   * @return the offset from the chunk's first block, from 0 to {@code CHUNK_SIZE - 1}
   */
  public static int blockToLocal(int block) {
    return Math.floorMod(block, CHUNK_SIZE);
  }
}
