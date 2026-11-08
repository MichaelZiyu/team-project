package terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OverworldGeneratorTest {

  private static final long SEED = 42L;
  private static final int SIZE = TerrainCoordinates.CHUNK_SIZE;

  /** Spacing between sampled blocks in the large-area tests, to keep them fast. */
  private static final int SAMPLE_STEP = 16;

  /** Half the width of the square sampled in the large-area tests, in blocks. */
  private static final int SAMPLE_RADIUS = 3000;

  @Test
  void sameSeedGivesSameChunks() {
    OverworldGenerator first = new OverworldGenerator(SEED);
    OverworldGenerator second = new OverworldGenerator(SEED);
    for (int chunk = -3; chunk <= 3; chunk++) {
      assertEquals(first.generate(chunk, -chunk), second.generate(chunk, -chunk));
    }
  }

  @Test
  void regeneratingAChunkGivesAnEqualChunk() {
    OverworldGenerator generator = new OverworldGenerator(SEED);
    assertEquals(generator.generate(5, -2), generator.generate(5, -2));
  }

  @Test
  void differentSeedsGiveDifferentChunks() {
    Chunk first = new OverworldGenerator(SEED).generate(0, 0);
    Chunk second = new OverworldGenerator(SEED + 1).generate(0, 0);
    assertNotEquals(first, second);
  }

  @Test
  void chunkTilesMatchBlockQueriesIncludingNegativeChunks() {
    // Tiles come from world positions alone, so this also proves neighbouring chunks line up.
    OverworldGenerator generator = new OverworldGenerator(SEED);
    int[][] chunkPositions = {{0, 0}, {-1, -1}, {-1, 2}, {3, -4}};
    for (int[] position : chunkPositions) {
      Chunk chunk = generator.generate(position[0], position[1]);
      assertEquals(position[0], chunk.getChunkX());
      assertEquals(position[1], chunk.getChunkY());
      for (int localX = 0; localX < SIZE; localX++) {
        for (int localY = 0; localY < SIZE; localY++) {
          int blockX = position[0] * SIZE + localX;
          int blockY = position[1] * SIZE + localY;
          assertEquals(generator.blockAt(blockX, blockY), chunk.blockAt(localX, localY));
          assertEquals(generator.heightAt(blockX, blockY), chunk.heightAt(localX, localY));
        }
      }
    }
  }

  @Test
  void waterIsExactlyTheHeightZeroTiles() {
    OverworldGenerator generator = new OverworldGenerator(SEED);
    forEachSample(
        (blockX, blockY) -> {
          boolean isWater = generator.blockAt(blockX, blockY) == BlockType.WATER;
          assertEquals(isWater, generator.heightAt(blockX, blockY) == 0);
        });
  }

  @Test
  void landBlocksMatchTheirBiome() {
    OverworldGenerator generator = new OverworldGenerator(SEED);
    forEachSample(
        (blockX, blockY) -> {
          int height = generator.heightAt(blockX, blockY);
          if (height == 0) {
            return;
          }
          BlockType block = generator.blockAt(blockX, blockY);
          switch (generator.biomeAt(blockX, blockY)) {
            case PLAINS:
              assertEquals(BlockType.GRASS, block);
              break;
            case DESERT:
              assertEquals(BlockType.SAND, block);
              break;
            case MOUNTAINS:
            default:
              BlockType expected =
                  height >= OverworldGenerator.SNOW_LINE ? BlockType.SNOW : BlockType.STONE;
              assertEquals(expected, block);
              break;
          }
        });
  }

  @Test
  void largeAreaContainsAllBiomesWaterAndHills() {
    OverworldGenerator generator = new OverworldGenerator(SEED);
    Set<Biome> biomes = EnumSet.noneOf(Biome.class);
    Set<BlockType> blocks = EnumSet.noneOf(BlockType.class);
    int[] maxHeight = {0};
    forEachSample(
        (blockX, blockY) -> {
          biomes.add(generator.biomeAt(blockX, blockY));
          blocks.add(generator.blockAt(blockX, blockY));
          maxHeight[0] = Math.max(maxHeight[0], generator.heightAt(blockX, blockY));
        });
    assertEquals(EnumSet.allOf(Biome.class), biomes);
    assertTrue(blocks.contains(BlockType.WATER), "no water found");
    assertTrue(blocks.contains(BlockType.SNOW), "no snowy peaks found");
    assertTrue(maxHeight[0] >= OverworldGenerator.SNOW_LINE, "tallest hill: " + maxHeight[0]);
  }

  /** Runs a check on a grid of blocks spread over a large area around the origin. */
  private static void forEachSample(BlockCheck check) {
    for (int blockX = -SAMPLE_RADIUS; blockX < SAMPLE_RADIUS; blockX += SAMPLE_STEP) {
      for (int blockY = -SAMPLE_RADIUS; blockY < SAMPLE_RADIUS; blockY += SAMPLE_STEP) {
        check.run(blockX, blockY);
      }
    }
  }

  /** A check applied to one block position. */
  @FunctionalInterface
  private interface BlockCheck {
    void run(int blockX, int blockY);
  }
}
