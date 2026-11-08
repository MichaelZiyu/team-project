package terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ChunkTest {

  private static final int SIZE = TerrainCoordinates.CHUNK_SIZE;

  /** Returns a builder with every tile set to grass at height 1. */
  private static Chunk.Builder filledBuilder(int chunkX, int chunkY) {
    Chunk.Builder builder = new Chunk.Builder(chunkX, chunkY);
    for (int x = 0; x < SIZE; x++) {
      for (int y = 0; y < SIZE; y++) {
        builder.setTile(x, y, BlockType.GRASS, 1);
      }
    }
    return builder;
  }

  @Test
  void buildKeepsChunkCoordinates() {
    Chunk chunk = filledBuilder(-3, 7).build();
    assertEquals(-3, chunk.getChunkX());
    assertEquals(7, chunk.getChunkY());
  }

  @Test
  void buildKeepsEachTile() {
    Chunk chunk =
        filledBuilder(0, 0)
            .setTile(0, 0, BlockType.WATER, 0)
            .setTile(SIZE - 1, SIZE - 1, BlockType.SNOW, 5)
            .build();
    assertEquals(BlockType.WATER, chunk.blockAt(0, 0));
    assertEquals(0, chunk.heightAt(0, 0));
    assertEquals(BlockType.SNOW, chunk.blockAt(SIZE - 1, SIZE - 1));
    assertEquals(5, chunk.heightAt(SIZE - 1, SIZE - 1));
    assertEquals(BlockType.GRASS, chunk.blockAt(3, 4));
  }

  @Test
  void builderChangesAfterBuildDoNotAffectChunk() {
    Chunk.Builder builder = filledBuilder(0, 0);
    Chunk chunk = builder.build();
    builder.setTile(2, 2, BlockType.STONE, 9);
    assertEquals(BlockType.GRASS, chunk.blockAt(2, 2));
    assertEquals(1, chunk.heightAt(2, 2));
  }

  @Test
  void buildFailsIfAnyTileUnset() {
    Chunk.Builder builder = new Chunk.Builder(0, 0).setTile(0, 0, BlockType.SAND, 1);
    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  void setTileRejectsNegativeHeight() {
    Chunk.Builder builder = new Chunk.Builder(0, 0);
    assertThrows(IllegalArgumentException.class, () -> builder.setTile(0, 0, BlockType.SAND, -1));
  }

  @Test
  void setTileRejectsNullType() {
    Chunk.Builder builder = new Chunk.Builder(0, 0);
    assertThrows(NullPointerException.class, () -> builder.setTile(0, 0, null, 1));
  }

  @Test
  void tileAccessOutsideChunkThrows() {
    Chunk chunk = filledBuilder(0, 0).build();
    assertThrows(IndexOutOfBoundsException.class, () -> chunk.blockAt(SIZE, 0));
    assertThrows(IndexOutOfBoundsException.class, () -> chunk.heightAt(0, -1));
  }

  @Test
  void chunksWithSameContentAreEqual() {
    Chunk first = filledBuilder(1, 2).build();
    Chunk second = filledBuilder(1, 2).build();
    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  void chunksDifferingInOneTileOrPositionAreNotEqual() {
    Chunk base = filledBuilder(1, 2).build();
    assertNotEquals(base, filledBuilder(1, 2).setTile(5, 5, BlockType.DIRT, 1).build());
    assertNotEquals(base, filledBuilder(1, 2).setTile(5, 5, BlockType.GRASS, 2).build());
    assertNotEquals(base, filledBuilder(1, 3).build());
  }
}
