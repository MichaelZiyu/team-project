package terrain;

/**
 * Read-only access to a world's terrain: the single entry point for the rest of the game.
 *
 * <p>Positions are in blocks. Convert pixel positions first with {@link
 * TerrainCoordinates#pixelToBlock(double)}. Terrain is the same every time for the same seed, so
 * it never needs saving. Implementations are safe to call from both the game loop and the
 * rendering thread.
 */
public interface Terrain {

  /**
   * Returns the block type on top of a position.
   *
   * @param blockX the block's x coordinate; may be negative
   * @param blockY the block's y coordinate; may be negative
   * @return the surface block at that position
   */
  BlockType blockAt(int blockX, int blockY);

  /**
   * Returns the terrace height of a position.
   *
   * @param blockX the block's x coordinate; may be negative
   * @param blockY the block's y coordinate; may be negative
   * @return 0 for water, otherwise the land height in blocks, at least 1
   */
  int heightAt(int blockX, int blockY);

  /**
   * Returns the biome of a position.
   *
   * @param blockX the block's x coordinate; may be negative
   * @param blockY the block's y coordinate; may be negative
   * @return the biome that position belongs to
   */
  Biome biomeAt(int blockX, int blockY);

  /**
   * Returns a whole chunk, for code that works on many tiles at once, such as drawing.
   *
   * @param chunkX the chunk column, in chunks; may be negative
   * @param chunkY the chunk row, in chunks; may be negative
   * @return the chunk at that position
   */
  Chunk chunkAt(int chunkX, int chunkY);

  /**
   * Returns where a new player should start: a land block close to the origin.
   *
   * @return the spawn position; always on land, and the same every time for this terrain
   */
  BlockPos spawnPoint();
}
