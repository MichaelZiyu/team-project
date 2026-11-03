package terrain;

/**
 * Generates the top-down overworld: terraced hills, water and biomes, all from one seed.
 *
 * <p>Three independent noise fields drive generation. <b>Temperature</b> and <b>moisture</b>
 * vary slowly and choose the biome: cold land is mountains, hot and dry land is desert, and
 * everything else is plains. <b>Elevation</b> varies faster and shapes the hills. Colder land
 * gets a larger height range, so terrain rises gradually into mountains instead of jumping at
 * biome borders. Elevation below a threshold becomes water at height 0.
 *
 * <p>Every value depends only on the seed and the block's world position, never on which chunk
 * is being generated, so neighbouring chunks always line up seamlessly.
 */
public final class OverworldGenerator implements ChunkGenerator {

  private static final int SIZE = TerrainCoordinates.CHUNK_SIZE;

  // Values mixed into the seed so each noise field is independent of the others.
  private static final long TEMPERATURE_SALT = 0x5DEECE66DL;
  private static final long MOISTURE_SALT = 0x2545F4914F6CDD1DL;

  /** Width, in blocks, of typical biome regions. */
  private static final double CLIMATE_SCALE = 400.0;

  private static final int CLIMATE_OCTAVES = 2;

  /** Width, in blocks, of typical hills. */
  private static final double ELEVATION_SCALE = 60.0;

  private static final int ELEVATION_OCTAVES = 4;

  /** Temperature below which land is the mountain biome. */
  private static final double MOUNTAIN_TEMPERATURE = -0.2;

  /** Temperature above which dry land is the desert biome. */
  private static final double DESERT_TEMPERATURE = 0.2;

  /** Moisture below which hot land is the desert biome. */
  private static final double DESERT_MOISTURE = 0.0;

  /** Temperature where land starts getting taller on the way into mountains. */
  private static final double RUGGED_START_TEMPERATURE = -0.05;

  /** Temperature where land reaches full mountain height. */
  private static final double RUGGED_FULL_TEMPERATURE = -0.35;

  /** Elevation below which land is water. */
  private static final double WATER_THRESHOLD = -0.2;

  /** Terrace levels gained per unit of elevation in flat biomes. */
  private static final double LOWLAND_HEIGHT_RANGE = 5.0;

  /** Terrace levels gained per unit of elevation in full mountains. */
  private static final double MOUNTAIN_HEIGHT_RANGE = 16.0;

  /** The lowest height at which mountain tops are covered in snow. */
  static final int SNOW_LINE = 8;

  private final PerlinNoise elevationNoise;
  private final PerlinNoise temperatureNoise;
  private final PerlinNoise moistureNoise;

  /**
   * Creates a generator for one world.
   *
   * @param seed the world seed; equal seeds always produce identical worlds
   */
  public OverworldGenerator(long seed) {
    this.elevationNoise = new PerlinNoise(seed);
    this.temperatureNoise = new PerlinNoise(seed ^ TEMPERATURE_SALT);
    this.moistureNoise = new PerlinNoise(seed ^ MOISTURE_SALT);
  }

  @Override
  public Chunk generate(int chunkX, int chunkY) {
    Chunk.Builder builder = new Chunk.Builder(chunkX, chunkY);
    int firstX = chunkX * SIZE;
    int firstY = chunkY * SIZE;
    for (int localX = 0; localX < SIZE; localX++) {
      for (int localY = 0; localY < SIZE; localY++) {
        int blockX = firstX + localX;
        int blockY = firstY + localY;
        builder.setTile(localX, localY, blockAt(blockX, blockY), heightAt(blockX, blockY));
      }
    }
    return builder.build();
  }

  /**
   * Returns the biome at a block.
   *
   * @param blockX the block's x coordinate; may be negative
   * @param blockY the block's y coordinate; may be negative
   * @return the biome that block belongs to
   */
  public Biome biomeAt(int blockX, int blockY) {
    double temperature = temperatureAt(blockX, blockY);
    if (temperature < MOUNTAIN_TEMPERATURE) {
      return Biome.MOUNTAINS;
    }
    if (temperature > DESERT_TEMPERATURE && moistureAt(blockX, blockY) < DESERT_MOISTURE) {
      return Biome.DESERT;
    }
    return Biome.PLAINS;
  }

  /**
   * Returns the terrace height at a block.
   *
   * @param blockX the block's x coordinate; may be negative
   * @param blockY the block's y coordinate; may be negative
   * @return 0 for water, otherwise the land height in blocks, at least 1
   */
  public int heightAt(int blockX, int blockY) {
    double elevation =
        elevationNoise.fractal(
            blockX / ELEVATION_SCALE, blockY / ELEVATION_SCALE, ELEVATION_OCTAVES);
    if (elevation < WATER_THRESHOLD) {
      return 0;
    }
    double ruggedness = ruggednessAt(temperatureAt(blockX, blockY));
    double heightRange =
        LOWLAND_HEIGHT_RANGE + ruggedness * (MOUNTAIN_HEIGHT_RANGE - LOWLAND_HEIGHT_RANGE);
    return 1 + (int) Math.floor((elevation - WATER_THRESHOLD) * heightRange);
  }

  /**
   * Returns the block type on top of a block position.
   *
   * @param blockX the block's x coordinate; may be negative
   * @param blockY the block's y coordinate; may be negative
   * @return water at height 0, otherwise the surface block for that biome and height
   */
  public BlockType blockAt(int blockX, int blockY) {
    int height = heightAt(blockX, blockY);
    if (height == 0) {
      return BlockType.WATER;
    }
    switch (biomeAt(blockX, blockY)) {
      case DESERT:
        return BlockType.SAND;
      case MOUNTAINS:
        return height >= SNOW_LINE ? BlockType.SNOW : BlockType.STONE;
      case PLAINS:
      default:
        return BlockType.GRASS;
    }
  }

  private double temperatureAt(int blockX, int blockY) {
    return temperatureNoise.fractal(
        blockX / CLIMATE_SCALE, blockY / CLIMATE_SCALE, CLIMATE_OCTAVES);
  }

  private double moistureAt(int blockX, int blockY) {
    return moistureNoise.fractal(blockX / CLIMATE_SCALE, blockY / CLIMATE_SCALE, CLIMATE_OCTAVES);
  }

  /** Returns 0 for warm land, 1 for full mountains, and a smooth blend in between. */
  private static double ruggednessAt(double temperature) {
    double progress =
        (RUGGED_START_TEMPERATURE - temperature)
            / (RUGGED_START_TEMPERATURE - RUGGED_FULL_TEMPERATURE);
    return Math.max(0, Math.min(1, progress));
  }
}
