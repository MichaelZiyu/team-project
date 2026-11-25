package weatherscene;

import terrain.PerlinNoise;

/**
 * How thick fog is at each point of the world: dense banks in some places, thin haze or clear
 * gaps in others, drifting slowly with the wind.
 *
 * <p>Fog is two layers of smooth noise ({@link PerlinNoise}) stretched sideways, so it lies in
 * long wisps like fog settling over fields. The layers drift east at different speeds, so as they
 * slide past each other the banks slowly change shape, grow and thin out, rather than the same
 * pattern simply sliding along. A curve then pushes the result apart into thick banks and clear
 * gaps, rather than an even grey.
 *
 * <p>Thickness belongs to the world, not the screen: a bank stays where it is as the player walks
 * through it, only moving with the wind.
 */
final class FogField {

  /** How wide the large fog banks are, roughly, in pixels. */
  static final double BANK_WIDTH_PX = 560;

  /** How tall the large fog banks are, roughly, in pixels; less than wide, so they lie in wisps. */
  static final double BANK_HEIGHT_PX = 260;

  /** How wide the smaller, faster wisps are, roughly, in pixels. */
  static final double WISP_WIDTH_PX = 300;

  /** How tall the smaller, faster wisps are, roughly, in pixels. */
  static final double WISP_HEIGHT_PX = 170;

  /** How fast the large banks drift east, in pixels per second. */
  static final double BANK_DRIFT_PX_PER_SECOND = 9;

  /** How fast the smaller wisps drift east, in pixels per second. */
  static final double WISP_DRIFT_PX_PER_SECOND = 23;

  /** The noise value at and below which fog is at its thinnest. */
  private static final double THIN_BELOW = -0.24;

  /** The noise value at and above which fog is at its densest. */
  private static final double DENSE_ABOVE = 0.3;

  private final PerlinNoise banks;
  private final PerlinNoise wisps;
  private double seconds;
  private double speed = 1;

  /**
   * Creates fog.
   *
   * @param seed seeds where the banks lie
   */
  FogField(long seed) {
    this.banks = new PerlinNoise(seed);
    this.wisps = new PerlinNoise(seed + 1);
  }

  /**
   * Makes the fog drift faster or slower than usual.
   *
   * @param speed compared with usual: 1 is usual
   * @throws IllegalArgumentException if {@code speed} is negative
   */
  void setSpeed(double speed) {
    if (!(speed >= 0)) {
      throw new IllegalArgumentException("speed must not be negative: " + speed);
    }
    this.speed = speed;
  }

  /**
   * Moves the fog forward in time.
   *
   * @param elapsed how much time passed, in seconds
   */
  void update(double elapsed) {
    seconds += elapsed * speed;
  }

  /**
   * Returns how thick the fog is at a point of the world right now.
   *
   * @param worldX the point's x position, in pixels
   * @param worldY the point's y position, in pixels
   * @return from 0 (its thinnest) to 1 (its densest)
   */
  double thickness(double worldX, double worldY) {
    double bank =
        banks.fractal(
            (worldX - BANK_DRIFT_PX_PER_SECOND * seconds) / BANK_WIDTH_PX,
            worldY / BANK_HEIGHT_PX,
            3);
    double wisp =
        wisps.fractal(
            (worldX - WISP_DRIFT_PX_PER_SECOND * seconds) / WISP_WIDTH_PX,
            worldY / WISP_HEIGHT_PX,
            2);
    double raw = 0.65 * bank + 0.35 * wisp;
    double t = Math.max(0, Math.min(1, (raw - THIN_BELOW) / (DENSE_ABOVE - THIN_BELOW)));
    // Smooth at both ends, so banks have soft edges.
    return t * t * (3 - 2 * t);
  }
}
