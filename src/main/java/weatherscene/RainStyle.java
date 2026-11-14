package weatherscene;

/**
 * How heavily and at what angle rain falls.
 *
 * @param drops how many drops fall at once over the ground around the screen; high drops appear
 *     beyond the screen's edges, so about a third of them are in view at any moment
 * @param fallSpeedPx how fast drops fall, in pixels per second
 * @param windEastPx how fast wind carries drops east while they fall, in pixels per second
 * @param windNorthPx how fast wind carries drops north while they fall, in pixels per second
 */
public record RainStyle(int drops, double fallSpeedPx, double windEastPx, double windNorthPx) {

  /** No rain: drops still in the air finish falling, and no new ones start. */
  public static final RainStyle NONE = new RainStyle(0, 1400, 0, 0);

  /** Light, steady rain falling almost straight down. */
  public static final RainStyle RAIN = new RainStyle(58, 1554, 40, 0);

  /** Heavy rain slanted by a strong wind. */
  public static final RainStyle THUNDERSTORM = new RainStyle(1670, 1700, 320, -60);

  /**
   * Creates a style.
   *
   * @throws IllegalArgumentException if {@code drops} is negative or {@code fallSpeedPx} is not
   *     positive
   */
  public RainStyle {
    if (drops < 0) {
      throw new IllegalArgumentException("drops must not be negative: " + drops);
    }
    if (!(fallSpeedPx > 0)) {
      throw new IllegalArgumentException("fallSpeedPx must be positive: " + fallSpeedPx);
    }
  }

  /**
   * Returns this rain made heavier or lighter, and faster or slower. The wind is unchanged.
   *
   * @param amount how many drops, compared with this style: 0 is none, 1 the same
   * @param speed how fast they fall, compared with this style: 1 is the same
   * @return the adjusted style
   * @throws IllegalArgumentException if {@code amount} is negative or {@code speed} is not
   *     positive
   */
  public RainStyle scaled(double amount, double speed) {
    if (!(amount >= 0)) {
      throw new IllegalArgumentException("amount must not be negative: " + amount);
    }
    if (!(speed > 0)) {
      throw new IllegalArgumentException("speed must be positive: " + speed);
    }
    return new RainStyle(
        (int) Math.round(drops * amount), fallSpeedPx * speed, windEastPx, windNorthPx);
  }
}
