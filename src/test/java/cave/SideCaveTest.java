package cave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import org.junit.jupiter.api.Test;
import terrain.BlockPos;

class SideCaveTest {

  private static final int W = SideCave.WIDTH_BLOCKS;
  private static final int H = SideCave.HEIGHT_BLOCKS;

  @Test
  void theSameSeedCarvesTheSameCave() {
    SideCave a = new SideCave(7);
    SideCave b = new SideCave(7);
    for (int x = 0; x < W; x++) {
      for (int y = 0; y < H; y++) {
        assertEquals(a.isSolid(x, y), b.isSolid(x, y));
      }
    }
  }

  @Test
  void theCaveIsWalledInAndOutsideIsSolid() {
    SideCave cave = new SideCave(8);
    for (int x = -1; x <= W; x++) {
      assertTrue(cave.isSolid(x, -1) && cave.isSolid(x, 0) && cave.isSolid(x, H - 1));
    }
    for (int y = 0; y < H; y++) {
      assertTrue(cave.isSolid(0, y) && cave.isSolid(W - 1, y) && cave.isSolid(W, y));
    }
  }

  @Test
  void thePlayerArrivesOnFlatLedgeWithRoomAbove() {
    for (long seed = 0; seed < 20; seed++) {
      SideCave cave = new SideCave(seed);
      BlockPos spawn = cave.spawnPoint();
      assertTrue(cave.isSolid(spawn.blockX(), spawn.blockY() - 1), "floor under the spawn");
      for (int y = 0; y < SideCave.SPAWN_HEADROOM; y++) {
        assertFalse(cave.isSolid(spawn.blockX(), spawn.blockY() + y), "room above the spawn");
      }
    }
  }

  @Test
  void openSpaceReachesFromTheSpawnAcrossTheWholeCave() {
    for (long seed = 0; seed < 20; seed++) {
      SideCave cave = new SideCave(seed);
      boolean[][] reached = openFrom(cave, cave.spawnPoint());
      int furthest = 0;
      int count = 0;
      for (int x = 0; x < W; x++) {
        for (int y = 0; y < H; y++) {
          if (reached[x][y]) {
            furthest = Math.max(furthest, x);
            count++;
          }
        }
      }
      assertTrue(furthest > W - 15, "seed " + seed + " reaches column " + furthest);
      assertTrue(count > W * 4, "seed " + seed + " has " + count + " open blocks");
    }
  }

  @Test
  void differentSeedsCarveDifferentCaves() {
    SideCave a = new SideCave(1);
    SideCave b = new SideCave(2);
    int different = 0;
    for (int x = 0; x < W; x++) {
      for (int y = 0; y < H; y++) {
        if (a.isSolid(x, y) != b.isSolid(x, y)) {
          different++;
        }
      }
    }
    assertTrue(different > 500);
  }

  /** The open blocks joined to a start block, side by side or one above another. */
  private static boolean[][] openFrom(SideCave cave, BlockPos start) {
    boolean[][] reached = new boolean[W][H];
    Deque<int[]> queue = new ArrayDeque<>();
    queue.add(new int[] {start.blockX(), start.blockY()});
    reached[start.blockX()][start.blockY()] = true;
    int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    while (!queue.isEmpty()) {
      int[] at = queue.poll();
      for (int[] step : steps) {
        int x = at[0] + step[0];
        int y = at[1] + step[1];
        if (x >= 0 && y >= 0 && x < W && y < H && !reached[x][y] && !cave.isSolid(x, y)) {
          reached[x][y] = true;
          queue.add(new int[] {x, y});
        }
      }
    }
    return reached;
  }
}
