package terrainscene;

import daynight.SunPosition;

/**
 * How the sun lights the terrain at one moment: which way and how far each level of height
 * throws its shadow, and how strong sunlight is.
 *
 * <p>Shadows point away from the sun. The higher the sun, the shorter they are; near the horizon
 * they would grow without limit, so their length is capped at {@link #MAX_LENGTH_PER_LEVEL_PX},
 * and by then they have faded out anyway because sunlight is weak.
 *
 * @param dxPerLevel how far east one level of height throws its shadow, in pixels (negative is
 *     west)
 * @param dyPerLevel how far north one level of height throws its shadow, in pixels (negative is
 *     south)
 * @param strength how strong sunlight is, from 0 (night: no shadows or sunlight) to 1
 */
public record ShadowCast(double dxPerLevel, double dyPerLevel, double strength) {

  /** The longest a shadow can be per level of height, in pixels: three terrace steps. */
  public static final double MAX_LENGTH_PER_LEVEL_PX = 3.0 * TileRenderable.STEP_HEIGHT_PX;

  /**
   * The lighting used when no time of day is given: a mid-morning sun to the right and a little
   * north, so each level throws a shadow 32 px left and 16 px south, in full sunlight.
   */
  public static final ShadowCast DEFAULT =
      new ShadowCast(-TileRenderable.STEP_HEIGHT_PX, -TileRenderable.STEP_HEIGHT_PX / 2.0, 1);

  /** Night: no sunlight, so no shadows, warmth or bright rims. */
  public static final ShadowCast NIGHT = new ShadowCast(0, 0, 0);

  /**
   * How directly an edge must face the sun to catch its full light: the cosine of the angle
   * between the edge's outward direction and the sun (0.5 is 60 degrees). Edges turned further
   * away catch less, fading smoothly to none at 90 degrees.
   */
  static final double FULL_LIGHT_FACING = 0.5;

  /**
   * Returns the lighting for a position of the sun.
   *
   * @param sun where the sun is
   * @return the matching lighting; {@link #NIGHT} when the sun is down
   */
  public static ShadowCast from(SunPosition sun) {
    double strength = sun.daylight();
    if (strength <= 0) {
      return NIGHT;
    }
    double length =
        Math.min(
            MAX_LENGTH_PER_LEVEL_PX, TileRenderable.STEP_HEIGHT_PX / Math.tan(sun.elevation()));
    return new ShadowCast(-sun.towardSunX() * length, -sun.towardSunY() * length, strength);
  }

  /**
   * Returns whether there is any sunlight at all.
   *
   * @return {@code false} at night
   */
  public boolean isDaylight() {
    return strength > 0;
  }

  /**
   * Returns how much sunlight a tile edge catches: none when it faces away from the sun or at
   * night, rising smoothly as it turns toward the sun, scaled by how strong sunlight is.
   *
   * @param edge an edge of a tile's top
   * @return from 0 (in shade) to 1 (facing the full sun)
   */
  public double edgeLight(Edge edge) {
    double length = Math.hypot(dxPerLevel, dyPerLevel);
    if (!isDaylight() || length == 0) {
      return 0;
    }
    // The sun is in the opposite direction to the shadows.
    double towardSunX = -dxPerLevel / length;
    double towardSunY = -dyPerLevel / length;
    double facing;
    switch (edge) {
      case BACK:
        facing = towardSunY;
        break;
      case FRONT:
        facing = -towardSunY;
        break;
      case LEFT:
        facing = -towardSunX;
        break;
      case RIGHT:
      default:
        facing = towardSunX;
        break;
    }
    double t = Math.max(0, Math.min(1, facing / FULL_LIGHT_FACING));
    // Smoothstep, so the change is gentle at both ends.
    return strength * t * t * (3 - 2 * t);
  }
}
