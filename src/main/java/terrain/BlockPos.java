package terrain;

/**
 * The position of one block, in block coordinates.
 *
 * <p>Convert to pixels with {@link TerrainCoordinates#blockToPixel(int)}.
 *
 * @param blockX the block's x coordinate; may be negative
 * @param blockY the block's y coordinate; may be negative
 */
public record BlockPos(int blockX, int blockY) {}
