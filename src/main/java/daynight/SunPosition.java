package daynight;

/**
 * Where the sun is in the sky, as seen from above the map.
 *
 * <p>The sun rises in the east (the right of the screen) at 6:00, passes the north (the top of
 * the screen) at noon, and sets in the west (the left) at 18:00, then continues below the
 * horizon through the night. Unlike the real sun it crosses the north side of the sky: this keeps
 * shadows falling toward the camera, where they can be seen, rather than hiding behind cliffs.
 *
 * <p>The shadow methods ({@link #shadowX}, {@link #shadowY}, {@link #shadowLength}) say where the
 * shadow of anything falls, so every shadow in the game (terrain, characters, items) points the
 * same way and has the same length for its height.
 *
 * @param azimuth the direction toward the sun, in radians counterclockwise from east: 0 is east,
 *     pi/2 north, pi west
 * @param elevation how high the sun is above the horizon, in radians; negative at night
 */
public record SunPosition(double azimuth, double elevation) {

  /** How high the sun climbs at noon, in radians (70 degrees). */
  public static final double MAX_ELEVATION = Math.toRadians(70);

  /**
   * How high the sun must be for full daylight, in radians (12 degrees). Below this, sunlight and
   * shadows fade toward dawn and dusk.
   */
  public static final double FULL_DAYLIGHT_ELEVATION = Math.toRadians(12);

  /** The time of sunrise, as a fraction of a day (6:00). */
  public static final double SUNRISE = 0.25;

  /**
   * The longest a shadow can be, as a multiple of the height of what casts it. Near the horizon
   * shadows would grow without limit; by the time they reach this cap they have mostly faded,
   * because daylight is weak.
   */
  public static final double MAX_SHADOW_LENGTH_RATIO = 3;

  /**
   * Returns the sun's position at a time of day.
   *
   * @param timeOfDay a fraction of a day: 0 is midnight, 0.5 is noon
   * @return the sun's position at that time
   */
  public static SunPosition at(double timeOfDay) {
    double angle = 2 * Math.PI * (timeOfDay - SUNRISE);
    double azimuth = angle - 2 * Math.PI * Math.floor(angle / (2 * Math.PI));
    return new SunPosition(azimuth, MAX_ELEVATION * Math.sin(angle));
  }

  /**
   * Returns how strong sunlight is: 0 at night, rising through dawn to 1 in full daylight.
   *
   * @return a strength from 0 to 1
   */
  public double daylight() {
    return Math.max(0, Math.min(1, elevation / FULL_DAYLIGHT_ELEVATION));
  }

  /**
   * Returns how long the shadow of something of the given height is: longer when the sun is low,
   * never more than {@link #MAX_SHADOW_LENGTH_RATIO} times the height, and 0 at night.
   *
   * @param height how tall the thing casting the shadow is, in any unit (for example pixels)
   * @return the shadow's length, in the same unit
   */
  public double shadowLength(double height) {
    if (daylight() <= 0) {
      return 0;
    }
    return Math.min(MAX_SHADOW_LENGTH_RATIO * height, height / Math.tan(elevation));
  }

  /**
   * Returns how far east the shadow of the top of something of the given height falls from its
   * base. Shadows point away from the sun: west in the morning, east in the evening.
   *
   * @param height how tall the thing casting the shadow is, in any unit (for example pixels)
   * @return the east-west offset, in the same unit; negative is west, 0 at night
   */
  public double shadowX(double height) {
    return -towardSunX() * shadowLength(height);
  }

  /**
   * Returns how far north the shadow of the top of something of the given height falls from its
   * base. The sun crosses the north of the sky, so this is 0 or negative (south) during the day.
   *
   * @param height how tall the thing casting the shadow is, in any unit (for example pixels)
   * @return the north-south offset, in the same unit; negative is south, 0 at night
   */
  public double shadowY(double height) {
    return -towardSunY() * shadowLength(height);
  }

  /**
   * Returns the east-west part of the direction toward the sun.
   *
   * @return from -1 (west) to 1 (east)
   */
  public double towardSunX() {
    return Math.cos(azimuth);
  }

  /**
   * Returns the north-south part of the direction toward the sun.
   *
   * @return from -1 (south) to 1 (north)
   */
  public double towardSunY() {
    return Math.sin(azimuth);
  }
}
