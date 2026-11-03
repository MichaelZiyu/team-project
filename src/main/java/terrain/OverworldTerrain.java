package terrain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The overworld's terrain, generated on demand and kept in a cache of recently used chunks.
 *
 * <p>Chunks are generated the first time they are needed. The most recently used chunks stay in
 * memory; older ones are dropped and simply regenerated if needed again, which gives an equal
 * chunk because generation is deterministic.
 */
public final class OverworldTerrain implements Terrain {

  /** How many chunks to keep in memory; far more than fit on screen at once. */
  static final int DEFAULT_CACHE_CHUNKS = 256;

  /** How far from the origin, in blocks, to look for land to spawn on. */
  static final int SPAWN_SEARCH_RADIUS = 2000;

  /** The cache's hash map load factor (Java's default). */
  private static final float CACHE_LOAD_FACTOR = 0.75f;

  /** Bits used for the chunk x coordinate when packing a chunk position into a cache key. */
  private static final int KEY_SHIFT = 32;

  private static final long LOW_32_BITS = 0xFFFFFFFFL;

  private final OverworldGenerator generator;
  private final Map<Long, Chunk> cache;
  private BlockPos spawn;

  /**
   * Creates terrain backed by a generator.
   *
   * @param generator the generator that creates this world's chunks
   */
  public OverworldTerrain(OverworldGenerator generator) {
    this(generator, DEFAULT_CACHE_CHUNKS);
  }

  /**
   * Creates terrain with a custom cache size. Small sizes are useful for testing eviction.
   *
   * @param generator the generator that creates this world's chunks
   * @param cacheChunks how many chunks to keep in memory; at least 1
   * @throws IllegalArgumentException if {@code cacheChunks} is less than 1
   */
  OverworldTerrain(OverworldGenerator generator, int cacheChunks) {
    if (cacheChunks < 1) {
      throw new IllegalArgumentException("cacheChunks must be at least 1: " + cacheChunks);
    }
    this.generator = generator;
    // Access order (the "true") keeps the least recently used chunk first, so it is evicted.
    this.cache =
        new LinkedHashMap<>(cacheChunks, CACHE_LOAD_FACTOR, true) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<Long, Chunk> eldest) {
            return size() > cacheChunks;
          }
        };
  }

  /**
   * Creates the overworld terrain for a seed.
   *
   * @param seed the world seed; equal seeds always produce identical terrain
   * @return the terrain for that seed
   */
  public static OverworldTerrain fromSeed(long seed) {
    return new OverworldTerrain(new OverworldGenerator(seed));
  }

  @Override
  public BlockType blockAt(int blockX, int blockY) {
    return chunkContaining(blockX, blockY)
        .blockAt(TerrainCoordinates.blockToLocal(blockX), TerrainCoordinates.blockToLocal(blockY));
  }

  @Override
  public int heightAt(int blockX, int blockY) {
    return chunkContaining(blockX, blockY)
        .heightAt(TerrainCoordinates.blockToLocal(blockX), TerrainCoordinates.blockToLocal(blockY));
  }

  @Override
  public Biome biomeAt(int blockX, int blockY) {
    return generator.biomeAt(blockX, blockY);
  }

  @Override
  public synchronized Chunk chunkAt(int chunkX, int chunkY) {
    return cache.computeIfAbsent(key(chunkX, chunkY), k -> generator.generate(chunkX, chunkY));
  }

  /**
   * {@inheritDoc}
   *
   * <p>Checks squares of growing size around the origin and picks the land block nearest the
   * origin in the first square that contains any.
   *
   * @throws IllegalStateException if there is no land within {@value #SPAWN_SEARCH_RADIUS}
   *     blocks of the origin
   */
  @Override
  public synchronized BlockPos spawnPoint() {
    if (spawn == null) {
      spawn = findSpawn();
    }
    return spawn;
  }

  private BlockPos findSpawn() {
    for (int radius = 0; radius <= SPAWN_SEARCH_RADIUS; radius++) {
      BlockPos best = null;
      // Only the edge of each square is new; the inside was already checked by smaller squares.
      for (int i = -radius; i <= radius; i++) {
        best = closerLand(best, i, -radius);
        best = closerLand(best, i, radius);
        best = closerLand(best, -radius, i);
        best = closerLand(best, radius, i);
      }
      if (best != null) {
        return best;
      }
    }
    throw new IllegalStateException("no land within " + SPAWN_SEARCH_RADIUS + " blocks of spawn");
  }

  /** Returns whichever is closer to the origin: {@code best}, or the given block if it is land. */
  private BlockPos closerLand(BlockPos best, int blockX, int blockY) {
    if (heightAt(blockX, blockY) == 0) {
      return best;
    }
    long distance = distanceSquared(blockX, blockY);
    if (best == null || distance < distanceSquared(best.blockX(), best.blockY())) {
      return new BlockPos(blockX, blockY);
    }
    return best;
  }

  private static long distanceSquared(int blockX, int blockY) {
    return (long) blockX * blockX + (long) blockY * blockY;
  }

  private Chunk chunkContaining(int blockX, int blockY) {
    int chunkX = TerrainCoordinates.blockToChunk(blockX);
    int chunkY = TerrainCoordinates.blockToChunk(blockY);
    return chunkAt(chunkX, chunkY);
  }

  /** Packs a chunk position into one {@code long} so it can be used as a map key. */
  private static long key(int chunkX, int chunkY) {
    return ((long) chunkX << KEY_SHIFT) | (chunkY & LOW_32_BITS);
  }
}
