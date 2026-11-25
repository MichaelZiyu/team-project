package terrain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where trees grow: scattered over the plains, in groves here and open fields there.
 *
 * <p>The world is split into square cells {@link #CELL_BLOCKS} blocks across, and each cell has
 * at most one tree, on a tile picked at random but kept away from the cell's edges, so no two
 * canopies ever overlap. Whether a cell has a tree depends on smooth noise over the world, so
 * trees cluster into groves. A tree only grows on a grass tile in the plains with flat ground all
 * around under its canopy, no water or cave entrance there, and nothing in front of it taller
 * than its own ground, which could hide its trunk.
 *
 * <p>Everything comes from the seed and the position, so the same trees always grow in the same
 * places. Safe to use from several threads at once.
 */
public final class Trees {

  /** How many blocks across each cell is; each has at most one tree. */
  static final int CELL_BLOCKS = 7;

  /** How many blocks across groves and open fields are, roughly. */
  static final double GROVE_BLOCKS = 36;

  /** The chance of a tree in a cell where trees are fewest. */
  static final double LEAST_CHANCE = 0.08;

  /** The chance of a tree in a cell in the thick of a grove. */
  static final double MOST_CHANCE = 0.9;

  private final Terrain terrain;
  private final long seed;
  private final PerlinNoise groves;
  private final Map<Long, Optional<Tree>> cells = new ConcurrentHashMap<>();

  /**
   * Creates the trees for a terrain.
   *
   * @param terrain the terrain they grow on
   * @param seed the world seed
   */
  public Trees(Terrain terrain, long seed) {
    this.terrain = terrain;
    this.seed = seed;
    this.groves = new PerlinNoise(seed ^ 0x7EE5L);
  }

  /**
   * Returns the trees whose trunks stand in a rectangle of tiles.
   *
   * @param minBlockX the rectangle's west edge, in blocks
   * @param minBlockY the rectangle's south edge, in blocks
   * @param maxBlockX the rectangle's east edge, in blocks, inclusive
   * @param maxBlockY the rectangle's north edge, in blocks, inclusive
   * @return the trees, in no particular order
   */
  public List<Tree> within(int minBlockX, int minBlockY, int maxBlockX, int maxBlockY) {
    List<Tree> found = new ArrayList<>();
    for (int cellX = Math.floorDiv(minBlockX, CELL_BLOCKS);
        cellX <= Math.floorDiv(maxBlockX, CELL_BLOCKS);
        cellX++) {
      for (int cellY = Math.floorDiv(minBlockY, CELL_BLOCKS);
          cellY <= Math.floorDiv(maxBlockY, CELL_BLOCKS);
          cellY++) {
        treeInCell(cellX, cellY)
            .filter(
                tree ->
                    tree.blockX() >= minBlockX
                        && tree.blockX() <= maxBlockX
                        && tree.blockY() >= minBlockY
                        && tree.blockY() <= maxBlockY)
            .ifPresent(found::add);
      }
    }
    return found;
  }

  /**
   * Returns the tree whose trunk stands on a tile, if any.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @return the tree, or empty if no trunk stands there
   */
  public Optional<Tree> at(int blockX, int blockY) {
    return treeInCell(Math.floorDiv(blockX, CELL_BLOCKS), Math.floorDiv(blockY, CELL_BLOCKS))
        .filter(tree -> tree.blockX() == blockX && tree.blockY() == blockY);
  }

  private Optional<Tree> treeInCell(int cellX, int cellY) {
    return cells.computeIfAbsent(
        (long) cellX << 32 | (cellY & 0xFFFFFFFFL), key -> growInCell(cellX, cellY));
  }

  /** Decides a cell's tree, if it has one. */
  private Optional<Tree> growInCell(int cellX, int cellY) {
    long hash = mix(seed + cellX * 0x9E3779B97F4A7C15L + cellY * 0xC2B2AE3D27D4EB4FL);
    Tree.Shape[] shapes = Tree.Shape.values();
    // Small trees most often, broad ones least.
    int pick = (int) Long.remainderUnsigned(hash >>> 40, 20);
    Tree.Shape shape = pick < 8 ? shapes[0] : pick < 15 ? shapes[1] : shapes[2];
    // Keep the canopy inside the cell, so neighbouring cells' canopies never meet.
    int room = CELL_BLOCKS - 2 * Tree.Shape.BROAD.radius();
    int blockX = cellX * CELL_BLOCKS + Tree.Shape.BROAD.radius()
        + (int) Long.remainderUnsigned(hash, room);
    int blockY = cellY * CELL_BLOCKS + Tree.Shape.BROAD.radius()
        + (int) Long.remainderUnsigned(hash >>> 16, room);
    double roll = (hash >>> 11 & 0xFFFF) / 65536.0;
    if (roll >= chanceAt(blockX, blockY) || !canGrow(blockX, blockY, shape.radius())) {
      return Optional.empty();
    }
    return Optional.of(new Tree(blockX, blockY, terrain.heightAt(blockX, blockY), shape));
  }

  /** Returns the chance of a tree around a tile: high in groves, low in open fields. */
  double chanceAt(int blockX, int blockY) {
    double noise = groves.fractal(blockX / GROVE_BLOCKS, blockY / GROVE_BLOCKS, 2);
    double grove = Math.max(0, Math.min(1, (noise + 0.25) / 0.6));
    return LEAST_CHANCE + (MOST_CHANCE - LEAST_CHANCE) * grove;
  }

  /** Returns whether a tree with a canopy of a given radius can stand on a tile. */
  private boolean canGrow(int blockX, int blockY, int radius) {
    if (terrain.biomeAt(blockX, blockY) != Biome.PLAINS
        || terrain.blockAt(blockX, blockY) != BlockType.GRASS) {
      return false;
    }
    int ground = terrain.heightAt(blockX, blockY);
    for (int dx = -radius; dx <= radius; dx++) {
      for (int dy = -radius; dy <= radius; dy++) {
        int x = blockX + dx;
        int y = blockY + dy;
        if (terrain.heightAt(x, y) != ground
            || terrain.blockAt(x, y) == BlockType.WATER
            || terrain.caveEntranceAt(x, y) != null) {
          return false;
        }
      }
      // Taller ground just in front would be drawn behind the tree, though it should hide it.
      if (terrain.heightAt(blockX + dx, blockY - radius - 1) > ground) {
        return false;
      }
    }
    return true;
  }

  /** Scrambles a number so that neighbouring cells get unrelated results. */
  private static long mix(long z) {
    z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
    z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
    return z ^ (z >>> 31);
  }
}
