package daynight;

/**
 * Where the sun is in the sky, as seen from above the map.
 *
 * <p>The sun rises in the east (the right of the screen) at 6:00, passes the north (the top of
 * the screen) at noon, and sets in the west (the left) at 18:00, then continues below the
 * horizon through the night. Unlike the real sun it crosses the north side of the sky: this keeps
 * shadows falling toward the camera, where they can be seen, rather than hiding behind cliffs.
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
