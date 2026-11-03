package terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class OverworldTerrainTest {

  private static final long SEED = 42L;

  /** Block coordinates on both sides of chunk edges, including around the origin. */
  private static final int[] EDGE_BLOCKS = {-33, -17, -16, -15, -1, 0, 1, 15, 16, 17, 31, 32};

  @Test
  void blockQueriesMatchGeneratorAcrossChunkEdges() {
    OverworldGenerator generator = new OverworldGenerator(SEED);
    Terrain terrain = new OverworldTerrain(generator);
    for (int blockX : EDGE_BLOCKS) {
      for (int blockY : EDGE_BLOCKS) {
        assertEquals(generator.blockAt(blockX, blockY), terrain.blockAt(blockX, blockY));
        assertEquals(generator.heightAt(blockX, blockY), terrain.heightAt(blockX, blockY));
        assertEquals(generator.biomeAt(blockX, blockY), terrain.biomeAt(blockX, blockY));
      }
    }
  }

  @Test
  void chunkAtMatchesGeneratorAndIsCached() {
    OverworldGenerator generator = new OverworldGenerator(SEED);
    Terrain terrain = new OverworldTerrain(generator);
    Chunk chunk = terrain.chunkAt(-2, 3);
    assertEquals(generator.generate(-2, 3), chunk);
    assertSame(chunk, terrain.chunkAt(-2, 3));
  }

  @Test
  void evictedChunksAreRegeneratedIdentically() {
    OverworldGenerator generator = new OverworldGenerator(SEED);
    Terrain terrain = new OverworldTerrain(generator, 2);
    Chunk first = terrain.chunkAt(0, 0);
    terrain.chunkAt(1, 0);
    terrain.chunkAt(2, 0);
    Chunk again = terrain.chunkAt(0, 0);
    assertEquals(first, again);
    for (int blockX : EDGE_BLOCKS) {
      assertEquals(generator.heightAt(blockX, 5), terrain.heightAt(blockX, 5));
    }
  }

  @Test
  void cacheSizeMustBePositive() {
    OverworldGenerator generator = new OverworldGenerator(SEED);
    assertThrows(IllegalArgumentException.class, () -> new OverworldTerrain(generator, 0));
  }

  @Test
  void fromSeedMatchesExplicitGenerator() {
    Terrain fromSeed = OverworldTerrain.fromSeed(SEED);
    Terrain explicit = new OverworldTerrain(new OverworldGenerator(SEED));
    assertEquals(explicit.chunkAt(4, -4), fromSeed.chunkAt(4, -4));
    assertEquals(explicit.spawnPoint(), fromSeed.spawnPoint());
  }

  @Test
  void spawnPointIsAlwaysOnLand() {
    for (long seed = 0; seed < 40; seed++) {
      Terrain terrain = OverworldTerrain.fromSeed(seed);
      BlockPos spawn = terrain.spawnPoint();
      assertTrue(
          terrain.heightAt(spawn.blockX(), spawn.blockY()) > 0, "seed " + seed + " spawned in water");
    }
  }

  @Test
  void spawnPointIsOriginWhenOriginIsLand() {
    for (long seed = 0; seed < 40; seed++) {
      Terrain terrain = OverworldTerrain.fromSeed(seed);
      if (terrain.heightAt(0, 0) > 0) {
        assertEquals(new BlockPos(0, 0), terrain.spawnPoint());
      }
    }
  }

  @Test
  void spawnPointIsStable() {
    Terrain terrain = OverworldTerrain.fromSeed(SEED);
    assertEquals(terrain.spawnPoint(), terrain.spawnPoint());
    assertEquals(terrain.spawnPoint(), OverworldTerrain.fromSeed(SEED).spawnPoint());
  }

  @Test
  void spawnPointIsNearestLandWhenOriginIsWater() {
    boolean checkedWaterOrigin = false;
    for (long seed = 0; seed < 200 && !checkedWaterOrigin; seed++) {
      Terrain terrain = OverworldTerrain.fromSeed(seed);
      if (terrain.heightAt(0, 0) > 0) {
        continue;
      }
      checkedWaterOrigin = true;
      BlockPos spawn = terrain.spawnPoint();
      int ring = Math.max(Math.abs(spawn.blockX()), Math.abs(spawn.blockY()));
      // Every block in the smaller squares around the origin must be water.
      for (int blockX = 1 - ring; blockX < ring; blockX++) {
        for (int blockY = 1 - ring; blockY < ring; blockY++) {
          assertEquals(0, terrain.heightAt(blockX, blockY), "closer land at " + blockX + ", " + blockY);
        }
      }
    }
    assertTrue(checkedWaterOrigin, "no seed in range starts in water; widen the range");
  }

  @Test
  void concurrentQueriesGiveConsistentResults() throws Exception {
    OverworldGenerator generator = new OverworldGenerator(SEED);
    Terrain terrain = new OverworldTerrain(generator, 8);
    ExecutorService pool = Executors.newFixedThreadPool(4);
    try {
      List<Future<Boolean>> results = new ArrayList<>();
      for (int thread = 0; thread < 4; thread++) {
        int offset = thread * 7;
        results.add(
            pool.submit(
                () -> {
                  for (int blockX = -200; blockX < 200; blockX += 3) {
                    int blockY = blockX + offset;
                    if (terrain.heightAt(blockX, blockY) != generator.heightAt(blockX, blockY)) {
                      return false;
                    }
                  }
                  return true;
                }));
      }
      for (Future<Boolean> result : results) {
        assertTrue(result.get());
      }
    } finally {
      pool.shutdownNow();
    }
  }
}
