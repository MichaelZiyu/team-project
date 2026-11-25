package terrain;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * The inside of a cave: winding tunnels and chambers in solid rock, generated from a seed (such as
 * a {@link CaveEntrance}'s), so the same entrance always leads to the same cave.
 *
 * <p>The cave fills a square of {@link #SIZE_BLOCKS} blocks centred on the origin; everything
 * outside it is rock. It is made the way many games make caves: start with rock scattered at
 * random, then repeatedly let each spot become rock if most of its neighbours are, and open if
 * most are open. That smooths the noise into natural-looking tunnels and rooms. Only the largest
 * connected open space is kept, so every part of the cave can be walked to. The floor rises and
 * falls gently in terraces ({@link #MAX_FLOOR} levels), and the rock around it stands {@link
 * #WALL_HEIGHT} levels high, so the walls show as tall cliffs.
 *
 * <p>Like the overworld, this is plain data: what it looks like is up to whoever draws it.
 */
public final class CaveTerrain implements Terrain {

  /** How wide and tall the cave's square is, in blocks. */
  public static final int SIZE_BLOCKS = 96;

  /** The height of the rock around the open space, in levels. */
  public static final int WALL_HEIGHT = 5;

  /** The highest the floor rises, in levels; the lowest is 1. */
  public static final int MAX_FLOOR = 3;

  /** How much of the cave starts out as rock, before smoothing. */
  static final double ROCK_SHARE = 0.47;

  /** How many rounds of smoothing turn the noise into tunnels. */
  static final int SMOOTHING_ROUNDS = 5;

  /** The least of the cave that must be open, or another layout is tried. */
  static final double MIN_OPEN_SHARE = 0.25;

  /** How many blocks across a terrace of floor typically is. */
  private static final double FLOOR_SCALE = 14;

  private static final int HALF = SIZE_BLOCKS / 2;

  private final boolean[][] open;
  private final int[][] floor;
  private final BlockPos spawn;

  /**
   * Generates a cave.
   *
   * @param seed decides the cave's layout; equal seeds always give identical caves
   */
  public CaveTerrain(long seed) {
    Random random = new Random(seed);
    boolean[][] layout;
    do {
      layout = largestOpening(smooth(scatter(random)));
    } while (openShare(layout) < MIN_OPEN_SHARE);
    this.open = layout;
    PerlinNoise noise = new PerlinNoise(random.nextLong());
    this.floor = new int[SIZE_BLOCKS][SIZE_BLOCKS];
    for (int x = 0; x < SIZE_BLOCKS; x++) {
      for (int y = 0; y < SIZE_BLOCKS; y++) {
        double value = noise.fractal(x / FLOOR_SCALE, y / FLOOR_SCALE, 2);
        // From about -0.5 to 0.5 across most of the cave, onto levels 1 to MAX_FLOOR.
        int level = 1 + (int) Math.floor((value + 0.5) * MAX_FLOOR);
        floor[x][y] = Math.max(1, Math.min(MAX_FLOOR, level));
      }
    }
    this.spawn = findSpawn();
  }

  /**
   * Generates the cave an entrance leads to.
   *
   * @param entrance the entrance
   * @return the cave, the same every time for the same entrance
   */
  public static CaveTerrain below(CaveEntrance entrance) {
    return new CaveTerrain(entrance.seed());
  }

  /**
   * Returns whether a tile is open floor, rather than rock.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @return {@code true} if it can be walked on
   */
  public boolean isOpen(int blockX, int blockY) {
    int x = blockX + HALF;
    int y = blockY + HALF;
    return x >= 0 && x < SIZE_BLOCKS && y >= 0 && y < SIZE_BLOCKS && open[x][y];
  }

  @Override
  public BlockType blockAt(int blockX, int blockY) {
    return BlockType.STONE;
  }

  @Override
  public int heightAt(int blockX, int blockY) {
    return isOpen(blockX, blockY) ? floor[blockX + HALF][blockY + HALF] : WALL_HEIGHT;
  }

  @Override
  public Biome biomeAt(int blockX, int blockY) {
    return Biome.CAVE;
  }

  @Override
  public Chunk chunkAt(int chunkX, int chunkY) {
    int size = TerrainCoordinates.CHUNK_SIZE;
    Chunk.Builder builder = new Chunk.Builder(chunkX, chunkY);
    for (int localX = 0; localX < size; localX++) {
      for (int localY = 0; localY < size; localY++) {
        int blockX = chunkX * size + localX;
        int blockY = chunkY * size + localY;
        builder.setTile(localX, localY, blockAt(blockX, blockY), heightAt(blockX, blockY));
      }
    }
    return builder.build();
  }

  /**
   * Returns where a player arriving in the cave stands: the open tile nearest its middle.
   *
   * @return the arrival tile
   */
  @Override
  public BlockPos spawnPoint() {
    return spawn;
  }

  /** Scatters rock at random, with a solid border so the cave is closed. */
  private static boolean[][] scatter(Random random) {
    boolean[][] cells = new boolean[SIZE_BLOCKS][SIZE_BLOCKS];
    for (int x = 1; x < SIZE_BLOCKS - 1; x++) {
      for (int y = 1; y < SIZE_BLOCKS - 1; y++) {
        cells[x][y] = random.nextDouble() >= ROCK_SHARE;
      }
    }
    return cells;
  }

  /** Smooths scattered rock into tunnels: a spot is rock if most of the 3 by 3 around it is. */
  private static boolean[][] smooth(boolean[][] cells) {
    boolean[][] current = cells;
    for (int round = 0; round < SMOOTHING_ROUNDS; round++) {
      boolean[][] next = new boolean[SIZE_BLOCKS][SIZE_BLOCKS];
      for (int x = 1; x < SIZE_BLOCKS - 1; x++) {
        for (int y = 1; y < SIZE_BLOCKS - 1; y++) {
          int rock = 0;
          for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
              if (!current[x + dx][y + dy]) {
                rock++;
              }
            }
          }
          next[x][y] = rock < 5;
        }
      }
      current = next;
    }
    return current;
  }

  /** Keeps only the largest connected open space; everything else becomes rock. */
  private static boolean[][] largestOpening(boolean[][] cells) {
    int[][] region = new int[SIZE_BLOCKS][SIZE_BLOCKS];
    int regions = 0;
    int best = 0;
    int bestSize = 0;
    for (int x = 0; x < SIZE_BLOCKS; x++) {
      for (int y = 0; y < SIZE_BLOCKS; y++) {
        if (cells[x][y] && region[x][y] == 0) {
          regions++;
          int size = fill(cells, region, x, y, regions);
          if (size > bestSize) {
            bestSize = size;
            best = regions;
          }
        }
      }
    }
    boolean[][] kept = new boolean[SIZE_BLOCKS][SIZE_BLOCKS];
    for (int x = 0; x < SIZE_BLOCKS; x++) {
      for (int y = 0; y < SIZE_BLOCKS; y++) {
        kept[x][y] = best != 0 && region[x][y] == best;
      }
    }
    return kept;
  }

  /** Marks a connected open space with a number, returning how many tiles it has. */
  private static int fill(boolean[][] cells, int[][] region, int startX, int startY, int number) {
    Deque<int[]> todo = new ArrayDeque<>();
    todo.push(new int[] {startX, startY});
    region[startX][startY] = number;
    int size = 0;
    while (!todo.isEmpty()) {
      int[] at = todo.pop();
      size++;
      int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
      for (int[] step : steps) {
        int x = at[0] + step[0];
        int y = at[1] + step[1];
        if (x >= 0 && x < SIZE_BLOCKS && y >= 0 && y < SIZE_BLOCKS
            && cells[x][y] && region[x][y] == 0) {
          region[x][y] = number;
          todo.push(new int[] {x, y});
        }
      }
    }
    return size;
  }

  private static double openShare(boolean[][] cells) {
    int count = 0;
    for (boolean[] column : cells) {
      for (boolean cell : column) {
        if (cell) {
          count++;
        }
      }
    }
    return count / (double) (SIZE_BLOCKS * SIZE_BLOCKS);
  }

  private BlockPos findSpawn() {
    BlockPos best = null;
    long bestDistance = Long.MAX_VALUE;
    for (int x = -HALF; x < HALF; x++) {
      for (int y = -HALF; y < HALF; y++) {
        long distance = (long) x * x + (long) y * y;
        if (isOpen(x, y) && distance < bestDistance) {
          bestDistance = distance;
          best = new BlockPos(x, y);
        }
      }
    }
    return best;
  }
}
