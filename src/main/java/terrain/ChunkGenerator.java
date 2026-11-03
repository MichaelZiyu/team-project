package terrain;

/**
 * Creates the terrain for any chunk of an infinite world.
 *
 * <p>Implementations must be deterministic: generating the same chunk twice gives equal chunks,
 * so terrain never has to be saved, only regenerated. Different kinds of world, such as the
 * overworld and caves, are different implementations of this interface (the Strategy pattern).
 */
public interface ChunkGenerator {

  /**
   * Generates one chunk.
   *
   * @param chunkX the chunk column, in chunks; may be negative
   * @param chunkY the chunk row, in chunks; may be negative
   * @return the chunk at that position, with every tile set
   */
  Chunk generate(int chunkX, int chunkY);
}
