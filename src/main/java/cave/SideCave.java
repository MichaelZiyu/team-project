package cave;

import java.util.Random;
import terrain.BlockPos;
import terrain.CaveEntrance;
import terrain.PerlinNoise;

/**
 * A cave seen from the side: a grid of blocks, each solid rock or open space, with gravity
 * pulling down (toward lower y). Behind every block is the cave's back wall, which is never solid.
 *
 * <p>The cave is {@link #WIDTH_BLOCKS} blocks wide and {@link #HEIGHT_BLOCKS} tall, with
 * {@code (0, 0)} its bottom-left block; everything outside it is solid. It is carved out of solid
 * rock: a main tunnel winding from the spawn ledge on the left to the far right, rising and falling
 * gently, with chambers along it, branches climbing up or down from it, and shafts dropping from
 * it to chambers below, to climb; and a lower tunnel under part of it, joined to it by shafts at
 * both ends. Smoothing then rounds the rock's edges, like water-worn stone.
 *
 * <p>The same seed always carves the same cave.
 */
public final class SideCave {

  /** How many blocks wide the cave is. */
  public static final int WIDTH_BLOCKS = 200;

  /** How many blocks tall the cave is. */
  public static final int HEIGHT_BLOCKS = 72;

  /** How thick the solid rock around the cave's edges is, in blocks; never carved. */
  static final int BORDER = 3;

  /** The spawn ledge's first and last columns, and how tall the space above it is. */
  static final int SPAWN_FIRST_X = 6;

  static final int SPAWN_LAST_X = 16;
  static final int SPAWN_HEADROOM = 5;

  private final boolean[][] solid = new boolean[WIDTH_BLOCKS][HEIGHT_BLOCKS];
  private final Random random;
  private final PerlinNoise noise;
  private final int[] tunnelFloor = new int[WIDTH_BLOCKS];
  private final BlockPos spawn;

  /**
   * Carves a cave.
   *
   * @param seed decides its shape
   */
  public SideCave(long seed) {
    this.random = new Random(seed);
    this.noise = new PerlinNoise(seed);
    for (boolean[] column : solid) {
      java.util.Arrays.fill(column, true);
    }
    int spawnFloor = HEIGHT_BLOCKS - 18;
    carveMainTunnel(spawnFloor);
    carveLowerTunnel();
    for (int i = 0; i < 8; i++) {
      carveChamber();
    }
    for (int i = 0; i < 6; i++) {
      carveBranch();
    }
    for (int i = 0; i < 3; i++) {
      carveShaft();
    }
    smooth();
    smooth();
    // A flat ledge to arrive on, with room above it.
    for (int x = SPAWN_FIRST_X; x <= SPAWN_LAST_X; x++) {
      solid[x][spawnFloor - 1] = true;
      for (int y = spawnFloor; y < spawnFloor + SPAWN_HEADROOM; y++) {
        solid[x][y] = false;
      }
    }
    this.spawn = new BlockPos((SPAWN_FIRST_X + SPAWN_LAST_X) / 2, spawnFloor);
  }

  /**
   * Returns the cave below a cave entrance.
   *
   * @param entrance the entrance
   * @return the cave; the same entrance always leads to the same cave
   */
  public static SideCave below(CaveEntrance entrance) {
    return new SideCave(entrance.seed());
  }

  /**
   * Returns whether a block is solid rock, which can be stood on and climbed but not walked
   * through.
   *
   * @param blockX the block's x coordinate, from the cave's left edge
   * @param blockY the block's y coordinate, from the cave's bottom edge
   * @return {@code true} for rock, and for anywhere outside the cave
   */
  public boolean isSolid(int blockX, int blockY) {
    if (blockX < 0 || blockY < 0 || blockX >= WIDTH_BLOCKS || blockY >= HEIGHT_BLOCKS) {
      return true;
    }
    return solid[blockX][blockY];
  }

  /**
   * Returns where the player arrives: an open block on the spawn ledge.
   *
   * @return the block whose bottom the player stands on
   */
  public BlockPos spawnPoint() {
    return spawn;
  }

  /** Carves the main tunnel, from the spawn ledge across the whole cave. */
  private void carveMainTunnel(int startFloor) {
    double floor = startFloor;
    int lowest = BORDER + 2;
    int highest = HEIGHT_BLOCKS - BORDER - 8;
    for (int x = BORDER; x < WIDTH_BLOCKS - BORDER; x++) {
      // Wander toward a target height that itself drifts slowly, never more than a block in
      // three columns, so the floor rises and falls in single steps.
      double target =
          HEIGHT_BLOCKS / 2.0 + (HEIGHT_BLOCKS / 2.0 - 14) * noise.fractal(x / 55.0, 1.3, 2) * 1.6;
      double step = Math.max(-0.34, Math.min(0.34, target - floor));
      floor = Math.max(lowest, Math.min(highest, floor + (x < SPAWN_LAST_X + 4 ? 0 : step)));
      int bottom = (int) Math.round(floor);
      int headroom = 4 + (int) Math.round(1.5 * (noise.noise(x / 9.0, 7.1) + 1));
      tunnelFloor[x] = bottom;
      open(x, bottom, x, bottom + headroom - 1);
    }
  }

  /**
   * Carves a second tunnel under part of the main one, winding along near the cave's floor, with a
   * shaft at each end up to the main tunnel.
   */
  private void carveLowerTunnel() {
    int start = SPAWN_LAST_X + 20 + random.nextInt(20);
    int end = WIDTH_BLOCKS - BORDER - 20 - random.nextInt(30);
    int[] floors = new int[WIDTH_BLOCKS];
    for (int x = start; x <= end; x++) {
      double wander = noise.fractal(x / 40.0, 9.7, 2);
      int floor = Math.max(BORDER + 2, (int) Math.round(BORDER + 6 + 6 * wander));
      // Stay well below the main tunnel.
      floor = Math.min(floor, tunnelFloor[x] - 10);
      floors[x] = floor;
      open(x, floor, x, floor + 3 + (int) Math.round(noise.noise(x / 8.0, 3.3) + 1));
    }
    for (int x : new int[] {start, end}) {
      open(x - 1, floors[x], x + 1, tunnelFloor[x]);
    }
  }

  /** Carves a chamber, a rounded room, somewhere along the main tunnel. */
  private void carveChamber() {
    int x = randomTunnelColumn();
    int radiusX = 5 + random.nextInt(5);
    int radiusY = 3 + random.nextInt(3);
    int centreY = tunnelFloor[x] + radiusY - 1;
    for (int dx = -radiusX; dx <= radiusX; dx++) {
      for (int dy = -radiusY; dy <= radiusY; dy++) {
        double distance = Math.pow(dx / (double) radiusX, 2) + Math.pow(dy / (double) radiusY, 2);
        if (distance <= 1) {
          openBlock(x + dx, centreY + dy);
        }
      }
    }
  }

  /** Carves a branch climbing up or down from the main tunnel, sideways like a stair. */
  private void carveBranch() {
    int x = randomTunnelColumn();
    int y = tunnelFloor[x];
    int across = random.nextBoolean() ? 1 : -1;
    int up = random.nextBoolean() ? 1 : -1;
    int length = 14 + random.nextInt(18);
    for (int i = 0; i < length; i++) {
      x += across;
      // One block up or down every two columns.
      if (i % 2 == 1) {
        y += up;
      }
      open(x, y, x, y + 3);
    }
  }

  /** Carves a shaft from the main tunnel's floor down to a small chamber, to climb. */
  private void carveShaft() {
    int x = randomTunnelColumn();
    int top = tunnelFloor[x];
    int depth = 9 + random.nextInt(9);
    int bottom = Math.max(BORDER + 1, top - depth);
    open(x - 1, bottom, x + 1, top);
    for (int dx = -6; dx <= 6; dx++) {
      for (int dy = 0; dy <= 3; dy++) {
        if (Math.abs(dx) + dy <= 8) {
          openBlock(x + dx, bottom + dy);
        }
      }
    }
  }

  private int randomTunnelColumn() {
    return SPAWN_LAST_X + 8 + random.nextInt(WIDTH_BLOCKS - SPAWN_LAST_X - 8 - BORDER - 8);
  }

  /**
   * Rounds the rock's edges: rock with mostly open neighbours opens, and open space with mostly
   * rock neighbours closes, as in a cellular automaton.
   */
  private void smooth() {
    boolean[][] next = new boolean[WIDTH_BLOCKS][HEIGHT_BLOCKS];
    for (int x = 0; x < WIDTH_BLOCKS; x++) {
      for (int y = 0; y < HEIGHT_BLOCKS; y++) {
        if (isBorder(x, y)) {
          next[x][y] = true;
          continue;
        }
        int rock = 0;
        for (int dx = -1; dx <= 1; dx++) {
          for (int dy = -1; dy <= 1; dy++) {
            if ((dx != 0 || dy != 0) && isSolid(x + dx, y + dy)) {
              rock++;
            }
          }
        }
        next[x][y] = rock >= 6 || (solid[x][y] && rock >= 4);
      }
    }
    for (int x = 0; x < WIDTH_BLOCKS; x++) {
      System.arraycopy(next[x], 0, solid[x], 0, HEIGHT_BLOCKS);
    }
  }

  private void open(int fromX, int fromY, int toX, int toY) {
    for (int x = Math.min(fromX, toX); x <= Math.max(fromX, toX); x++) {
      for (int y = Math.min(fromY, toY); y <= Math.max(fromY, toY); y++) {
        openBlock(x, y);
      }
    }
  }

  private void openBlock(int x, int y) {
    if (x >= 0 && y >= 0 && x < WIDTH_BLOCKS && y < HEIGHT_BLOCKS && !isBorder(x, y)) {
      solid[x][y] = false;
    }
  }

  private static boolean isBorder(int x, int y) {
    return x < BORDER || y < BORDER || x >= WIDTH_BLOCKS - BORDER || y >= HEIGHT_BLOCKS - BORDER;
  }
}
