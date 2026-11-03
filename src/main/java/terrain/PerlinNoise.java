package terrain;

/**
 * Seeded two-dimensional Perlin noise: a smooth, repeatable pseudo-random field.
 *
 * <p>Nearby points get similar values and distant points get unrelated ones, which makes noise
 * suitable for natural-looking terrain such as hills and biome regions. The same seed and point
 * always give the same value, on any machine, so terrain can be regenerated instead of saved.
 *
 * <p>How it works: the plane is divided into a grid of unit squares. Each grid corner gets a
 * pseudo-random direction (its gradient), chosen by hashing the seed with the corner's position.
 * For a point inside a square, each corner contributes how far the point lies along that
 * corner's direction, and the four contributions are blended with a smooth curve. Values are 0
 * exactly at grid corners and vary smoothly in between.
 */
public final class PerlinNoise {

  /** Scales raw 2D Perlin output, which lies within about ±0.707, to fill [-1, 1]. */
  private static final double OUTPUT_SCALE = Math.sqrt(2);

  private static final double DIAGONAL = Math.sqrt(0.5);

  /** The eight gradient directions a grid corner can have, all of length 1. */
  private static final double[][] GRADIENTS = {
    {1, 0}, {-1, 0}, {0, 1}, {0, -1},
    {DIAGONAL, DIAGONAL}, {-DIAGONAL, DIAGONAL}, {DIAGONAL, -DIAGONAL}, {-DIAGONAL, -DIAGONAL}
  };

  /** Shifts a 64-bit hash down to its top 3 bits, picking one of the eight gradients. */
  private static final int GRADIENT_SHIFT = 61;

  // Large odd constants used to spread grid positions across all 64 bits before mixing.
  private static final long X_MULTIPLIER = 0x9E3779B97F4A7C15L;
  private static final long Y_MULTIPLIER = 0xC2B2AE3D27D4EB4FL;

  // Constants of the SplitMix64 finalizer, a well-tested bit mixer.
  private static final long MIX_MULTIPLIER_1 = 0xBF58476D1CE4E5B9L;
  private static final long MIX_MULTIPLIER_2 = 0x94D049BB133111EBL;
  private static final int MIX_SHIFT_1 = 30;
  private static final int MIX_SHIFT_2 = 27;
  private static final int MIX_SHIFT_3 = 31;

  /** How much weaker each successive octave is in {@link #fractal}. */
  private static final double PERSISTENCE = 0.5;

  /** How much finer each successive octave is in {@link #fractal}. */
  private static final double LACUNARITY = 2.0;

  /**
   * How far each octave is shifted in {@link #fractal}. Not a whole number, so no octave samples
   * a grid corner at the origin, where noise is always exactly 0.
   */
  private static final double OCTAVE_OFFSET = 0.6180339887;

  private final long seed;

  /**
   * Creates noise for a seed.
   *
   * @param seed any value; equal seeds always produce identical noise
   */
  public PerlinNoise(long seed) {
    this.seed = seed;
  }

  /**
   * Returns the noise value at a point.
   *
   * <p>One grid square is one unit wide, so callers usually divide block coordinates by a scale
   * first: dividing by a larger number gives broader, gentler features.
   *
   * @param x the x coordinate; any finite value, including negatives
   * @param y the y coordinate; any finite value, including negatives
   * @return a value in [-1, 1]; exactly 0 at whole-number coordinates
   */
  public double noise(double x, double y) {
    long cellX = (long) Math.floor(x);
    long cellY = (long) Math.floor(y);
    double offsetX = x - cellX;
    double offsetY = y - cellY;

    double bottomLeft = cornerContribution(cellX, cellY, offsetX, offsetY);
    double bottomRight = cornerContribution(cellX + 1, cellY, offsetX - 1, offsetY);
    double topLeft = cornerContribution(cellX, cellY + 1, offsetX, offsetY - 1);
    double topRight = cornerContribution(cellX + 1, cellY + 1, offsetX - 1, offsetY - 1);

    double blendX = fade(offsetX);
    double blendY = fade(offsetY);
    double bottom = lerp(bottomLeft, bottomRight, blendX);
    double top = lerp(topLeft, topRight, blendX);
    double value = lerp(bottom, top, blendY) * OUTPUT_SCALE;
    return Math.max(-1, Math.min(1, value));
  }

  /**
   * Returns layered noise with both large shapes and small detail.
   *
   * <p>Sums several octaves of {@link #noise}: each octave is twice as fine and half as strong
   * as the one before. One octave gives smooth rolling shapes; more octaves add rougher detail.
   *
   * @param x the x coordinate; any finite value, including negatives
   * @param y the y coordinate; any finite value, including negatives
   * @param octaves how many layers to sum; at least 1
   * @return a value in [-1, 1]
   * @throws IllegalArgumentException if {@code octaves} is less than 1
   */
  public double fractal(double x, double y, int octaves) {
    if (octaves < 1) {
      throw new IllegalArgumentException("octaves must be at least 1: " + octaves);
    }
    double total = 0;
    double amplitude = 1;
    double frequency = 1;
    double maxTotal = 0;
    for (int i = 0; i < octaves; i++) {
      // Shift each octave differently so layers don't line up, and none hits a corner at (0, 0).
      double shift = (i + 1) * OCTAVE_OFFSET;
      total += noise(x * frequency + shift, y * frequency + shift) * amplitude;
      maxTotal += amplitude;
      amplitude *= PERSISTENCE;
      frequency *= LACUNARITY;
    }
    return total / maxTotal;
  }

  /** Returns how far the offset from a grid corner points along that corner's gradient. */
  private double cornerContribution(long cornerX, long cornerY, double offsetX, double offsetY) {
    double[] gradient = GRADIENTS[(int) (hash(cornerX, cornerY) >>> GRADIENT_SHIFT)];
    return gradient[0] * offsetX + gradient[1] * offsetY;
  }

  /** Returns a pseudo-random 64-bit value determined only by the seed and a grid corner. */
  private long hash(long cornerX, long cornerY) {
    long h = mix(seed ^ (cornerX * X_MULTIPLIER));
    return mix(h ^ (cornerY * Y_MULTIPLIER));
  }

  /** Scrambles bits so that similar inputs give unrelated outputs (SplitMix64 finalizer). */
  private static long mix(long value) {
    long z = value;
    z = (z ^ (z >>> MIX_SHIFT_1)) * MIX_MULTIPLIER_1;
    z = (z ^ (z >>> MIX_SHIFT_2)) * MIX_MULTIPLIER_2;
    return z ^ (z >>> MIX_SHIFT_3);
  }

  /** Eases 0..1 so blends have no visible creases at grid lines: 6t^5 - 15t^4 + 10t^3. */
  private static double fade(double t) {
    return t * t * t * (t * (t * 6 - 15) + 10);
  }

  /** Returns the value a fraction {@code t} of the way from {@code a} to {@code b}. */
  private static double lerp(double a, double b, double t) {
    return a + t * (b - a);
  }
}
