package terrain;

import java.util.List;

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

  /**
   * Returns the cave entrance covering a tile, if there is one there. Terrain without caves has
   * none.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @return the entrance, or {@code null} if the tile is not part of one
   */
  default CaveEntrance caveEntranceAt(int blockX, int blockY) {
    return null;
  }

  /**
   * Returns the cave entrances near a point, nearest first; for example, to offer the player a way
   * in when they come close to one. Terrain without caves has none.
   *
   * @param worldX the point's x position, in world pixels
   * @param worldY the point's y position, in world pixels
   * @param radiusPx how far from the point an entrance's middle may be, in world pixels
   * @return the entrances found, nearest first
   */
  default List<CaveEntrance> caveEntrancesNear(
      double worldX, double worldY, double radiusPx) {
    return List.of();
  }
}
