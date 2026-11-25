package terrainscene;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import terrain.BlockType;
import terrain.Terrain;
import terrain.TerrainCoordinates;

/**
 * Paints the water's surface, coloured by how deep it is.
 *
 * <p>The terrain stores no depth, so depth is taken from how far a point is from the nearest
 * shore: pale turquoise at the water's edge, darkening to deep blue {@link
 * #DEEP_BLOCKS} blocks out. The colour changes in steps of {@link #TEXEL_PX} pixels, the size of a
 * pixel in the tile textures, so the water matches their pixel-art look; each of those steps is
 * also brightened or darkened very slightly. The moving ripples on top are {@link WaterRipples}.
 *
 * <p>Every colour depends only on its position in the world, never on which rectangle is being
 * painted, so neighbouring chunks, or a chunk and the tiles in it painted one by one, always meet
 * without a seam. Land is left transparent. Safe to use from several threads at once.
 */
final class WaterPainter {

  /** How far from the shore water is fully deep, in blocks. */
  static final double DEEP_BLOCKS = 6;

  /** The size of each step of colour, in pixels: one pixel of a 16-pixel texture on a tile. */
  static final int TEXEL_PX = 4;

  /** The colour of water at the very edge of the shore. */
  static final Color SHORE_COLOUR = new Color(104, 184, 194);

  /** The colour of shallow water, {@link #SHALLOW_BLOCKS} blocks from the shore. */
  static final Color SHALLOW_COLOUR = new Color(52, 128, 170);

  /** The colour of deep water, {@link #DEEP_BLOCKS} blocks or more from the shore. */
  static final Color DEEP_COLOUR = new Color(22, 58, 112);

  /** How far from the shore water has the {@link #SHALLOW_COLOUR}, in blocks. */
  static final double SHALLOW_BLOCKS = 1.5;

  /** How much each step of colour is brightened or darkened at most, as a share. */
  static final double TEXTURE_STRENGTH = 0.035;

  /**
   * How many points per block the distance to shore is worked out at; colours in between are
   * blended from the nearest of them, which is far cheaper than working it out for every step.
   */
  static final int SAMPLES_PER_BLOCK = 4;

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  /** How many blocks around a point to look for shore: enough to find any within the deep range. */
  private static final int SEARCH_BLOCKS = (int) Math.ceil(DEEP_BLOCKS) + 1;

  private final Terrain terrain;

  /**
   * Creates a painter.
   *
   * @param terrain the terrain whose water to paint
   */
  WaterPainter(Terrain terrain) {
    this.terrain = terrain;
  }

  /**
   * Returns whether a tile is water.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @return {@code true} if its surface is water
   */
  boolean isWater(int blockX, int blockY) {
    return terrain.blockAt(blockX, blockY) == BlockType.WATER;
  }

  /**
   * Paints the water in a rectangle of tiles.
   *
   * @param firstX the left column of tiles, in blocks
   * @param firstY the bottom row of tiles, in blocks
   * @param widthBlocks how many columns to paint
   * @param heightBlocks how many rows to paint
   * @return an image exactly covering the tiles (north at the top), transparent over land; {@code
   *     null} if none of the tiles is water
   */
  BufferedImage paint(int firstX, int firstY, int widthBlocks, int heightBlocks) {
    boolean[][] water = new boolean[widthBlocks][heightBlocks];
    boolean anyWater = false;
    for (int x = 0; x < widthBlocks; x++) {
      for (int y = 0; y < heightBlocks; y++) {
        water[x][y] = isWater(firstX + x, firstY + y);
        anyWater |= water[x][y];
      }
    }
    if (!anyWater) {
      return null;
    }
    double[][] distances = sampleDistances(firstX, firstY, widthBlocks, heightBlocks);
    int texelsPerBlock = BLOCK / TEXEL_PX;
    int widthPx = widthBlocks * BLOCK;
    int heightPx = heightBlocks * BLOCK;
    int[] pixels = new int[widthPx * heightPx];
    for (int texelX = 0; texelX < widthBlocks * texelsPerBlock; texelX++) {
      for (int texelY = 0; texelY < heightBlocks * texelsPerBlock; texelY++) {
        if (!water[texelX / texelsPerBlock][texelY / texelsPerBlock]) {
          continue;
        }
        // Measured from the rectangle's lower-left corner, in samples, at the texel's centre.
        double sampleX = (texelX + 0.5) * SAMPLES_PER_BLOCK / texelsPerBlock;
        double sampleY = (texelY + 0.5) * SAMPLES_PER_BLOCK / texelsPerBlock;
        double distance = blend(distances, sampleX, sampleY);
        long worldTexelX = (long) firstX * texelsPerBlock + texelX;
        long worldTexelY = (long) firstY * texelsPerBlock + texelY;
        int colour = colourAt(distance, texture(worldTexelX, worldTexelY));
        fillTexel(pixels, widthPx, texelX * TEXEL_PX, heightPx - (texelY + 1) * TEXEL_PX, colour);
      }
    }
    BufferedImage image = new BufferedImage(widthPx, heightPx, BufferedImage.TYPE_INT_ARGB);
    image.setRGB(0, 0, widthPx, heightPx, pixels, 0, widthPx);
    return image;
  }

  /**
   * Returns how far a point is from the nearest land, in blocks, up to {@link #DEEP_BLOCKS}.
   *
   * @param x the point's x coordinate, in blocks (0.5 is the middle of tile 0)
   * @param y the point's y coordinate, in blocks
   * @return 0 on land or at its edge, up to {@link #DEEP_BLOCKS} far out
   */
  double shoreDistance(double x, double y) {
    int blockX = (int) Math.floor(x);
    int blockY = (int) Math.floor(y);
    double nearest = DEEP_BLOCKS;
    for (int east = -SEARCH_BLOCKS; east <= SEARCH_BLOCKS; east++) {
      for (int north = -SEARCH_BLOCKS; north <= SEARCH_BLOCKS; north++) {
        if (!isWater(blockX + east, blockY + north)) {
          nearest = Math.min(nearest, distanceToTile(x, y, blockX + east, blockY + north));
        }
      }
    }
    return nearest;
  }

  /**
   * Returns the colour of water a distance from the shore, brightened or darkened by a share.
   *
   * @param distance how far from the shore, in blocks
   * @param texture how much to brighten (positive) or darken (negative), as a share
   * @return the colour, as an opaque ARGB value
   */
  static int colourAt(double distance, double texture) {
    Color from;
    Color to;
    double t;
    if (distance < SHALLOW_BLOCKS) {
      from = SHORE_COLOUR;
      to = SHALLOW_COLOUR;
      t = distance / SHALLOW_BLOCKS;
    } else {
      from = SHALLOW_COLOUR;
      to = DEEP_COLOUR;
      t = Math.min(1, (distance - SHALLOW_BLOCKS) / (DEEP_BLOCKS - SHALLOW_BLOCKS));
    }
    // Smoothstep, so each band blends gently into the next.
    double s = t * t * (3 - 2 * t);
    double light = 1 + texture;
    int red = channel(from.getRed(), to.getRed(), s, light);
    int green = channel(from.getGreen(), to.getGreen(), s, light);
    int blue = channel(from.getBlue(), to.getBlue(), s, light);
    return 0xFF000000 | red << 16 | green << 8 | blue;
  }

  /**
   * Works out the distance to shore at every sample point of a rectangle of tiles, plus one row
   * and column past it so points near its far edges can still be blended. Land is found once for
   * the whole rectangle and its surroundings, rather than once per point.
   */
  private double[][] sampleDistances(int firstX, int firstY, int widthBlocks, int heightBlocks) {
    int landWidth = widthBlocks + 2 * SEARCH_BLOCKS + 1;
    int landHeight = heightBlocks + 2 * SEARCH_BLOCKS + 1;
    boolean[][] land = new boolean[landWidth][landHeight];
    for (int x = 0; x < landWidth; x++) {
      for (int y = 0; y < landHeight; y++) {
        land[x][y] = !isWater(firstX - SEARCH_BLOCKS + x, firstY - SEARCH_BLOCKS + y);
      }
    }
    int samplesX = widthBlocks * SAMPLES_PER_BLOCK + 1;
    int samplesY = heightBlocks * SAMPLES_PER_BLOCK + 1;
    double[][] distances = new double[samplesX][samplesY];
    for (int i = 0; i < samplesX; i++) {
      for (int j = 0; j < samplesY; j++) {
        double x = firstX + (double) i / SAMPLES_PER_BLOCK;
        double y = firstY + (double) j / SAMPLES_PER_BLOCK;
        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y);
        double nearest = DEEP_BLOCKS;
        for (int east = -SEARCH_BLOCKS; east <= SEARCH_BLOCKS; east++) {
          for (int north = -SEARCH_BLOCKS; north <= SEARCH_BLOCKS; north++) {
            int landX = blockX + east - firstX + SEARCH_BLOCKS;
            int landY = blockY + north - firstY + SEARCH_BLOCKS;
            if (land[landX][landY]) {
              nearest = Math.min(nearest, distanceToTile(x, y, blockX + east, blockY + north));
            }
          }
        }
        distances[i][j] = nearest;
      }
    }
    return distances;
  }

  /** Returns the distance from a point to the nearest part of a tile, in blocks. */
  private static double distanceToTile(double x, double y, int blockX, int blockY) {
    double dx = Math.max(0, Math.max(blockX - x, x - (blockX + 1)));
    double dy = Math.max(0, Math.max(blockY - y, y - (blockY + 1)));
    return Math.hypot(dx, dy);
  }

  /** Blends the four sample points around a position, measured in samples. */
  private static double blend(double[][] samples, double x, double y) {
    int left = (int) Math.floor(x);
    int bottom = (int) Math.floor(y);
    double across = x - left;
    double up = y - bottom;
    double lower = samples[left][bottom] * (1 - across) + samples[left + 1][bottom] * across;
    double upper =
        samples[left][bottom + 1] * (1 - across) + samples[left + 1][bottom + 1] * across;
    return lower * (1 - up) + upper * up;
  }

  /**
   * Returns how much one step of colour is brightened or darkened: a fixed, random-looking share
   * for each position in the world, up to {@link #TEXTURE_STRENGTH} either way.
   */
  private static double texture(long texelX, long texelY) {
    long z = texelX * 0x9E3779B97F4A7C15L + texelY * 0xC2B2AE3D27D4EB4FL;
    z = (z ^ (z >>> 31)) * 0xBF58476D1CE4E5B9L;
    z ^= z >>> 29;
    double unit = (z >>> 11) / (double) (1L << 53);
    return (2 * unit - 1) * TEXTURE_STRENGTH;
  }

  private static int channel(int from, int to, double t, double light) {
    double value = (from + (to - from) * t) * light;
    return (int) Math.max(0, Math.min(255, Math.round(value)));
  }

  private static void fillTexel(int[] pixels, int widthPx, int left, int top, int colour) {
    for (int row = top; row < top + TEXEL_PX; row++) {
      int start = row * widthPx + left;
      Arrays.fill(pixels, start, start + TEXEL_PX, colour);
    }
  }
}
