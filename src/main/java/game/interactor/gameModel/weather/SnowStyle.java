package game.interactor.gameModel.weather;

/**
 * How heavily and how fast snow falls, and how the wind drives it.
 *
 * @param flakes how many flakes fall at once over the ground around the screen; high flakes appear
 *     beyond the screen's edges, so only some of them are in view at any moment
 * @param fallSpeedPx how fast flakes fall on average, in pixels per second; each flake is a little
 *     faster or slower
 * @param windEastPx how fast wind carries flakes east while they fall, in pixels per second
 * @param windNorthPx how fast wind carries flakes north while they fall, in pixels per second
 * @param swayPx how far flakes sway from side to side as they fall, in pixels
 */
public record SnowStyle(
    int flakes, double fallSpeedPx, double windEastPx, double windNorthPx, double swayPx) {

  /** No snow: flakes still in the air finish falling, and no new ones start. */
  public static final SnowStyle NONE = new SnowStyle(0, 110, 0, 0, 0);

  /** Gentle snow, drifting down and swaying. */
  public static final SnowStyle SNOW = new SnowStyle(500, 110, 18, 0, 22);

  /** A blizzard: thick, fast snow driven hard sideways by the wind. */
  public static final SnowStyle BLIZZARD = new SnowStyle(2200, 320, 460, -60, 10);

  /**
   * Creates a style.
   *
   * @throws IllegalArgumentException if {@code flakes} or {@code swayPx} is negative, or {@code
   *     fallSpeedPx} is not positive
   */
  public SnowStyle {
    if (flakes < 0) {
      throw new IllegalArgumentException("flakes must not be negative: " + flakes);
    }
    if (!(fallSpeedPx > 0)) {
      throw new IllegalArgumentException("fallSpeedPx must be positive: " + fallSpeedPx);
    }
    if (!(swayPx >= 0)) {
      throw new IllegalArgumentException("swayPx must not be negative: " + swayPx);
    }
  }

  /**
   * Returns this snow made heavier or lighter, and faster or slower. The wind and sway are
   * unchanged.
   *
   * @param amount how many flakes, compared with this style: 0 is none, 1 the same
   * @param speed how fast they fall, compared with this style: 1 is the same
   * @return the adjusted style
   * @throws IllegalArgumentException if {@code amount} is negative or {@code speed} is not
   *     positive
   */
  public SnowStyle scaled(double amount, double speed) {
    if (!(amount >= 0)) {
      throw new IllegalArgumentException("amount must not be negative: " + amount);
    }
    if (!(speed > 0)) {
      throw new IllegalArgumentException("speed must be positive: " + speed);
    }
    return new SnowStyle(
        (int) Math.round(flakes * amount), fallSpeedPx * speed, windEastPx, windNorthPx, swayPx);
  }
}
