package terrain;

import java.util.Arrays;
import java.util.Objects;

/**
 * An immutable square of terrain, {@value TerrainCoordinates#CHUNK_SIZE} blocks on each side.
 *
 * <p>Each tile stores the block type on top and its terrace height, where 0 is water level and
 * each step up is one block. Tiles are addressed by local coordinates from 0 to {@code
 * CHUNK_SIZE - 1}; local {@code (0, 0)} is the chunk's lower-left corner. Create chunks with a
 * {@link Builder}.
 */
public final class Chunk {

  private static final int SIZE = TerrainCoordinates.CHUNK_SIZE;

  private final int chunkX;
  private final int chunkY;
  private final BlockType[][] blocks;
  private final int[][] heights;

  private Chunk(int chunkX, int chunkY, BlockType[][] blocks, int[][] heights) {
    this.chunkX = chunkX;
    this.chunkY = chunkY;
    this.blocks = blocks;
    this.heights = heights;
  }

  /**
   * Returns this chunk's x coordinate, in chunks.
   *
   * @return the chunk column; chunk 0 starts at block 0
   */
  public int getChunkX() {
    return chunkX;
  }

  /**
   * Returns this chunk's y coordinate, in chunks.
   *
   * @return the chunk row; chunk 0 starts at block 0
   */
  public int getChunkY() {
    return chunkY;
  }

  /**
   * Returns the block type on top of a tile.
   *
   * @param localX the tile's column inside this chunk, from 0 to {@code CHUNK_SIZE - 1}
   * @param localY the tile's row inside this chunk, from 0 to {@code CHUNK_SIZE - 1}
   * @return the block type at that tile
   * @throws IndexOutOfBoundsException if either coordinate is outside the chunk
   */
  public BlockType blockAt(int localX, int localY) {
    checkLocal(localX, localY);
    return blocks[localX][localY];
  }

  /**
   * Returns the terrace height of a tile.
   *
   * @param localX the tile's column inside this chunk, from 0 to {@code CHUNK_SIZE - 1}
   * @param localY the tile's row inside this chunk, from 0 to {@code CHUNK_SIZE - 1}
   * @return the height in blocks above water level; never negative
   * @throws IndexOutOfBoundsException if either coordinate is outside the chunk
   */
  public int heightAt(int localX, int localY) {
    checkLocal(localX, localY);
    return heights[localX][localY];
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Chunk other = (Chunk) obj;
    return chunkX == other.chunkX
        && chunkY == other.chunkY
        && Arrays.deepEquals(blocks, other.blocks)
        && Arrays.deepEquals(heights, other.heights);
  }

  @Override
  public int hashCode() {
    return Objects.hash(chunkX, chunkY, Arrays.deepHashCode(blocks), Arrays.deepHashCode(heights));
  }

  @Override
  public String toString() {
    return "Chunk(" + chunkX + ", " + chunkY + ")";
  }

  private static void checkLocal(int localX, int localY) {
    Objects.checkIndex(localX, SIZE);
    Objects.checkIndex(localY, SIZE);
  }

  /**
   * Builds a {@link Chunk} one tile at a time.
   *
   * <p>Every tile must be set before {@link #build()} is called. The builder can keep being used
   * after building; later changes never affect chunks it has already built.
   */
  public static final class Builder {

    private final int chunkX;
    private final int chunkY;
    private final BlockType[][] blocks = new BlockType[SIZE][SIZE];
    private final int[][] heights = new int[SIZE][SIZE];

    /**
     * Starts building the chunk at the given chunk coordinates.
     *
     * @param chunkX the chunk column, in chunks; may be negative
     * @param chunkY the chunk row, in chunks; may be negative
     */
    public Builder(int chunkX, int chunkY) {
      this.chunkX = chunkX;
      this.chunkY = chunkY;
    }

    /**
     * Sets one tile's block type and height.
     *
     * @param localX the tile's column inside the chunk, from 0 to {@code CHUNK_SIZE - 1}
     * @param localY the tile's row inside the chunk, from 0 to {@code CHUNK_SIZE - 1}
     * @param type the block type on top of the tile; must not be null
     * @param height the terrace height above water level; must not be negative
     * @return this builder, so calls can be chained
     * @throws IndexOutOfBoundsException if either coordinate is outside the chunk
     * @throws IllegalArgumentException if {@code height} is negative
     */
    public Builder setTile(int localX, int localY, BlockType type, int height) {
      checkLocal(localX, localY);
      Objects.requireNonNull(type, "type");
      if (height < 0) {
        throw new IllegalArgumentException("height must not be negative: " + height);
      }
      blocks[localX][localY] = type;
      heights[localX][localY] = height;
      return this;
    }

    /**
     * Returns a chunk containing the tiles set so far.
     *
     * @return a new immutable chunk
     * @throws IllegalStateException if any tile has not been set
     */
    public Chunk build() {
      BlockType[][] blocksCopy = new BlockType[SIZE][];
      int[][] heightsCopy = new int[SIZE][];
      for (int x = 0; x < SIZE; x++) {
        for (int y = 0; y < SIZE; y++) {
          if (blocks[x][y] == null) {
            throw new IllegalStateException("tile (" + x + ", " + y + ") was never set");
          }
        }
        blocksCopy[x] = blocks[x].clone();
        heightsCopy[x] = heights[x].clone();
      }
      return new Chunk(chunkX, chunkY, blocksCopy, heightsCopy);
    }
  }
}
