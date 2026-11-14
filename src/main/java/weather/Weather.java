package weather;

/**
 * The kinds of weather the game world can have, and how each one changes the light.
 *
 * <p>Clouds block the sun: {@link #cloudCover()} says how much, so sunlit things (bright terrain
 * rims, shadows, the warm glow on the ground) can fade under cloud. Bad weather also looks greyer:
 * {@link #saturation()} says how much colour is left. Both are plain numbers, so whatever draws the
 * world decides how to apply them.
 */
public enum Weather {
  /** No precipitation: clear or cloudy skies. */
  CLEAR(0, 1),
  /** Rain falling, with splashes on the ground. */
  RAIN(0.85, 0.55),
  /** Snow falling gently. */
  SNOW(0.7, 0.6),
  /** Heavy, wind-driven rain under a dark sky, with lightning. */
  THUNDERSTORM(1, 0.4),
  /** Heavy, wind-driven snow that hides the distance. */
  BLIZZARD(1, 0.35);

  private final double cloudCover;
  private final double saturation;

  Weather(double cloudCover, double saturation) {
    this.cloudCover = cloudCover;
    this.saturation = saturation;
  }

  /**
   * Returns how much of the sun the clouds block. Sunlight reaching the ground is the sun's own
   * strength times {@code 1 - cloudCover()}.
   *
   * @return from 0 (clear sky) to 1 (no sunlight gets through)
   */
  public double cloudCover() {
    return cloudCover;
  }

  /**
   * Returns how much colour the world keeps in this weather.
   *
   * @return from 0 (grey) to 1 (full colour)
   */
  public double saturation() {
    return saturation;
  }
}
