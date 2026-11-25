package terrain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the cave entrances are in a world.
 *
 * <p>The world is divided into square regions of {@link #REGION_BLOCKS} blocks, and each region
 * has at most one entrance, with a chance of {@link #CHANCE}. Its size, which way it runs, and
 * where in the region it lies are random, but follow from the world seed and the region alone, so
 * a world always has the same entrances. An entrance is only placed on dry land that is flat all
 * over it and one tile around it, so it never cuts into a cliff or a shore; a region whose chosen
 * spot is not flat has no entrance. Regions are worked out when first asked about, then
 * remembered. Safe to use from several threads at once.
 */
final class CaveEntrances {

  /** How wide and tall each region with at most one entrance is, in blocks. */
  static final int REGION_BLOCKS = 40;

  /** How likely a region is to try for an entrance. */
  static final double CHANCE = 0.6;

  /** The fewest tiles an entrance stretches along its length. */
  static final int MIN_LENGTH = 3;

  /** The most tiles an entrance stretches along its length. */
  static final int MAX_LENGTH = 5;

  /** The fewest tiles an entrance spans across. */
  static final int MIN_WIDTH = 2;

  /** The most tiles an entrance spans across. */
  static final int MAX_WIDTH = 3;

  /** How many tiles around an entrance must be as flat as the entrance itself. */
  private static final int FLAT_BORDER = 1;

  /** Mixed into the world seed so entrances do not line up with anything else from the seed. */
  private static final long SALT = 0x6A09E667F3BCC909L;

  private final Terrain terrain;
  private final long seed;
  private final Map<Long, Optional<CaveEntrance>> regions = new ConcurrentHashMap<>();

  /**
   * Creates the entrances of a world.
   *
   * @param terrain the world's terrain, to keep entrances on flat land
   * @param seed the world seed
   */
  CaveEntrances(Terrain terrain, long seed) {
    this.terrain = terrain;
    this.seed = seed ^ SALT;
  }

  /**
   * Returns the entrance covering a tile.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @return the entrance, or {@code null} if the tile is not part of one
   */
  CaveEntrance at(int blockX, int blockY) {
    CaveEntrance entrance =
        inRegion(Math.floorDiv(blockX, REGION_BLOCKS), Math.floorDiv(blockY, REGION_BLOCKS));
    return entrance != null && entrance.covers(blockX, blockY) ? entrance : null;
  }

  /**
   * Returns the entrances whose middles are within a distance of a point, nearest first.
   *
   * @param worldX the point's x position, in world pixels
   * @param worldY the point's y position, in world pixels
   * @param radiusPx how far to look, in world pixels
   * @return the entrances found
   */
  List<CaveEntrance> near(double worldX, double worldY, double radiusPx) {
    int regionPx = REGION_BLOCKS * TerrainCoordinates.BLOCK_SIZE_PX;
    int minRegionX = (int) Math.floor((worldX - radiusPx) / regionPx);
    int maxRegionX = (int) Math.floor((worldX + radiusPx) / regionPx);
    int minRegionY = (int) Math.floor((worldY - radiusPx) / regionPx);
    int maxRegionY = (int) Math.floor((worldY + radiusPx) / regionPx);
    List<CaveEntrance> found = new ArrayList<>();
    for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
      for (int regionY = minRegionY; regionY <= maxRegionY; regionY++) {
        CaveEntrance entrance = inRegion(regionX, regionY);
        if (entrance != null && entrance.distanceTo(worldX, worldY) <= radiusPx) {
          found.add(entrance);
        }
      }
    }
    found.sort(Comparator.comparingDouble(entrance -> entrance.distanceTo(worldX, worldY)));
    return found;
  }

  /** Returns a region's entrance, working it out the first time. */
  private CaveEntrance inRegion(int regionX, int regionY) {
    long key = (long) regionX << 32 | (regionY & 0xFFFFFFFFL);
    return regions.computeIfAbsent(key, k -> Optional.ofNullable(plan(regionX, regionY)))
        .orElse(null);
  }

  /** Decides a region's entrance, if it has one. */
  private CaveEntrance plan(int regionX, int regionY) {
    RandomBits random = new RandomBits(seed, regionX, regionY);
    if (random.nextDouble() >= CHANCE) {
      return null;
    }
    int length = MIN_LENGTH + random.nextInt(MAX_LENGTH - MIN_LENGTH + 1);
    int width = MIN_WIDTH + random.nextInt(MAX_WIDTH - MIN_WIDTH + 1);
    boolean eastWest = random.nextDouble() < 0.5;
    int widthBlocks = eastWest ? length : width;
    int heightBlocks = eastWest ? width : length;
    // Kept clear of the region's edges, so neighbouring regions' entrances never touch.
    int margin = FLAT_BORDER + 1;
    int minX =
        regionX * REGION_BLOCKS
            + margin
            + random.nextInt(REGION_BLOCKS - widthBlocks - 2 * margin + 1);
    int minY =
        regionY * REGION_BLOCKS
            + margin
            + random.nextInt(REGION_BLOCKS - heightBlocks - 2 * margin + 1);
    CaveEntrance entrance =
        new CaveEntrance(minX, minY, widthBlocks, heightBlocks, random.nextLong());
    return isFlatLand(entrance) ? entrance : null;
  }

  /** Returns whether an entrance and the tiles around it are all dry land at one height. */
  private boolean isFlatLand(CaveEntrance entrance) {
    int height = terrain.heightAt(entrance.minX(), entrance.minY());
    if (height < 1) {
      return false;
    }
    for (int x = entrance.minX() - FLAT_BORDER;
        x < entrance.minX() + entrance.widthBlocks() + FLAT_BORDER;
        x++) {
      for (int y = entrance.minY() - FLAT_BORDER;
          y < entrance.minY() + entrance.heightBlocks() + FLAT_BORDER;
          y++) {
        if (terrain.heightAt(x, y) != height || terrain.blockAt(x, y) == BlockType.WATER) {
          return false;
        }
      }
    }
    return true;
  }

  /** A small random number source made from the seed and a region, so regions are independent. */
  private static final class RandomBits {
    private long state;

    RandomBits(long seed, int regionX, int regionY) {
      state = seed + regionX * 0x9E3779B97F4A7C15L + regionY * 0xC2B2AE3D27D4EB4FL;
    }

    long nextLong() {
      state += 0x9E3779B97F4A7C15L;
      long z = state;
      z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
      z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
      return z ^ (z >>> 31);
    }

    double nextDouble() {
      return (nextLong() >>> 11) / (double) (1L << 53);
    }

    int nextInt(int bound) {
      return (int) Long.remainderUnsigned(nextLong(), bound);
    }
  }
}
