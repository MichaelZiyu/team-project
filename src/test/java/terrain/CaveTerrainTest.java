package terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CaveTerrainTest {

  private static final int HALF = CaveTerrain.SIZE_BLOCKS / 2;

  private final CaveTerrain cave = new CaveTerrain(1234L);

  @Test
  void theCaveIsClosedInByRock() {
    for (int i = -HALF - 3; i < HALF + 3; i++) {
      assertTrue(!cave.isOpen(i, -HALF) && !cave.isOpen(i, HALF - 1));
      assertTrue(!cave.isOpen(-HALF, i) && !cave.isOpen(HALF - 1, i));
      assertTrue(!cave.isOpen(i, HALF + 10) && !cave.isOpen(HALF + 10, i), "outside is rock");
    }
  }

  @Test
  void everyOpenTileCanBeWalkedToFromWherePlayersArrive() {
    BlockPos spawn = cave.spawnPoint();
    assertTrue(cave.isOpen(spawn.blockX(), spawn.blockY()));
    Set<Long> reached = new HashSet<>();
    Deque<int[]> todo = new ArrayDeque<>();
    todo.push(new int[] {spawn.blockX(), spawn.blockY()});
    reached.add(key(spawn.blockX(), spawn.blockY()));
    while (!todo.isEmpty()) {
      int[] at = todo.pop();
      int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
      for (int[] step : steps) {
        int x = at[0] + step[0];
        int y = at[1] + step[1];
        if (cave.isOpen(x, y) && reached.add(key(x, y))) {
          todo.push(new int[] {x, y});
        }
      }
    }
    int open = 0;
    for (int x = -HALF; x < HALF; x++) {
      for (int y = -HALF; y < HALF; y++) {
        if (cave.isOpen(x, y)) {
          open++;
        }
      }
    }
    assertEquals(open, reached.size(), "no open pocket is cut off");
    assertTrue(
        open >= CaveTerrain.MIN_OPEN_SHARE * CaveTerrain.SIZE_BLOCKS * CaveTerrain.SIZE_BLOCKS);
  }

  @Test
  void floorsAreLowTerracesAndRockStandsTall() {
    Set<Integer> floorLevels = new HashSet<>();
    for (int x = -HALF; x < HALF; x++) {
      for (int y = -HALF; y < HALF; y++) {
        int height = cave.heightAt(x, y);
        if (cave.isOpen(x, y)) {
          assertTrue(height >= 1 && height <= CaveTerrain.MAX_FLOOR);
          floorLevels.add(height);
        } else {
          assertEquals(CaveTerrain.WALL_HEIGHT, height);
        }
        assertEquals(BlockType.STONE, cave.blockAt(x, y));
        assertEquals(Biome.CAVE, cave.biomeAt(x, y));
      }
    }
    assertTrue(floorLevels.size() >= 2, "the floor should rise and fall");
  }

  @Test
  void chunksMatchTheTiles() {
    Chunk chunk = cave.chunkAt(-1, 0);
    for (int x = 0; x < TerrainCoordinates.CHUNK_SIZE; x++) {
      for (int y = 0; y < TerrainCoordinates.CHUNK_SIZE; y++) {
        int blockX = -TerrainCoordinates.CHUNK_SIZE + x;
        assertEquals(cave.heightAt(blockX, y), chunk.heightAt(x, y));
      }
    }
  }

  @Test
  void anEntranceAlwaysLeadsToTheSameCaveAndOthersToOtherCaves() {
    CaveEntrance entrance = new CaveEntrance(0, 0, 3, 2, 1234L);
    CaveTerrain again = CaveTerrain.below(entrance);
    CaveTerrain other = new CaveTerrain(99L);
    int same = 0;
    int differ = 0;
    for (int x = -HALF; x < HALF; x++) {
      for (int y = -HALF; y < HALF; y++) {
        assertEquals(cave.heightAt(x, y), again.heightAt(x, y));
        if (cave.isOpen(x, y) != other.isOpen(x, y)) {
          differ++;
        } else {
          same++;
        }
      }
    }
    assertTrue(differ > same / 4, "another seed gives a different cave");
    assertNotEquals(cave.spawnPoint(), null);
  }

  private static long key(int x, int y) {
    return (long) x << 32 | (y & 0xFFFFFFFFL);
  }
}
