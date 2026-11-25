package terrainscene;

/**
 * How the terrain is lit: which way and how far shadows fall, how strong the sunlight is, and any
 * extra brightening of tile tops or darkening of terrace faces.
 *
 * <p>The terrain is painted for one light at a time; changing it repaints the chunks (see {@link
 * ChunkedTerrainScene#setLight}). The everyday light is {@link #DEFAULT}, a mid-morning sun. Cloud
 * dims it ({@link #overcast}), and a lightning strike replaces it for a split second ({@link
 * #lightningStrike}).
 *
 * @param dxPerLevel how far east one level of height throws its shadow, in pixels (negative is
 *     west)
 * @param dyPerLevel how far north one level of height throws its shadow, in pixels (negative is
 *     south)
 * @param sunlight how strong sunlight is, from 0 to 1: it lights the warm wash on tile tops and the
 *     bright rims on edges facing the light
 * @param shadowStrength how dark shadows are, compared with an ordinary sunny day's: 0 is none, 1
 *     is usual, up to {@link #MAX_SHADOW_STRENGTH}
 * @param softShadows whether shadow edges are soft (from several light positions) or sharp
 * @param topGlow how much tile tops are brightened, from 0 (not at all) to 1 (fully white)
 * @param faceDarkening how much terrace faces are darkened, from 0 (not at all) to 1 (black)
 * @param golden how strongly a low sun gilds the edges facing it, at golden hour: from 0 (not at
 *     all) to 1 (a bright orange-yellow); see {@link #edgeGlow}
 */
public record TerrainLight(
    double dxPerLevel,
    double dyPerLevel,
    double sunlight,
    double shadowStrength,
    boolean softShadows,
    double topGlow,
    double faceDarkening,
    double golden) {

  /** The darkest shadows can be, compared with an ordinary sunny day's. */
  public static final double MAX_SHADOW_STRENGTH = 2;

  /**
   * The everyday light: a mid-morning sun to the right and a little north, so each level throws a
   * soft shadow 32 px left and 16 px south, in full sunlight.
   */
  public static final TerrainLight DEFAULT =
      new TerrainLight(
          -TileRenderable.STEP_HEIGHT_PX, -TileRenderable.STEP_HEIGHT_PX / 2.0, 1, 1, true, 0, 0);

  /** No light from above: no shadows, warmth or bright rims. */
  public static final TerrainLight NIGHT = new TerrainLight(0, 0, 0, 0, true, 0, 0);

  /** How far a lightning strike's shadows reach per level of height, in pixels. */
  public static final double STRIKE_SHADOW_PER_LEVEL_PX = 1.5 * TileRenderable.STEP_HEIGHT_PX;

  /**
   * How much of golden hour's glow the edges facing down the screen catch, whichever way the sun
   * is, compared with an edge facing straight at the sun.
   */
  public static final double FRONT_GLOW_SHARE = 0.25;

  /** How dark a lightning strike's shadows are, compared with an ordinary sunny day's. */
  public static final double STRIKE_SHADOW_STRENGTH = 1.5;

  /** How much a lightning strike brightens tile tops. */
  public static final double STRIKE_TOP_GLOW = 0.5;

  /** How much a lightning strike darkens terrace faces. */
  public static final double STRIKE_FACE_DARKENING = 0.7;

  /**
   * How directly an edge must face the light to catch all of it: the cosine of the angle between
   * the edge's outward direction and the light (0.5 is 60 degrees). Edges turned further away
   * catch less, fading smoothly to none at 90 degrees.
   */
  static final double FULL_LIGHT_FACING = 0.5;

  /**
   * Creates a light.
   *
   * @throws IllegalArgumentException if a strength or amount is out of range
   */
  public TerrainLight {
    checkRange("golden", golden, 1);
    checkRange("sunlight", sunlight, 1);
    checkRange("shadowStrength", shadowStrength, MAX_SHADOW_STRENGTH);
    checkRange("topGlow", topGlow, 1);
    checkRange("faceDarkening", faceDarkening, 1);
  }
  /**
   * Creates a light without golden hour's glow on the edges.
   *
   * @param dxPerLevel how far east one level throws its shadow, in pixels
   * @param dyPerLevel how far north one level throws its shadow, in pixels
   * @param sunlight how strong sunlight is, from 0 to 1
   * @param shadowStrength how dark shadows are, compared with an ordinary sunny day's
   * @param softShadows whether shadow edges are soft
   * @param topGlow how much tile tops are brightened, from 0 to 1
   * @param faceDarkening how much terrace faces are darkened, from 0 to 1
   * @throws IllegalArgumentException if a strength or amount is out of range
   */
  public TerrainLight(
      double dxPerLevel,
      double dyPerLevel,
      double sunlight,
      double shadowStrength,
      boolean softShadows,
      double topGlow,
      double faceDarkening) {
    this(dxPerLevel, dyPerLevel, sunlight, shadowStrength, softShadows, topGlow, faceDarkening, 0);
  }

  /**
   * Returns this light with golden hour's glow on the edges facing the sun.
   *
   * @param amount how strongly the edges are gilded, from 0 (not at all) to 1 (fully)
   * @return the light, otherwise the same
   * @throws IllegalArgumentException if {@code amount} is out of range
   */
  public TerrainLight withGolden(double amount) {
    return new TerrainLight(
        dxPerLevel, dyPerLevel, sunlight, shadowStrength, softShadows, topGlow, faceDarkening,
        amount);
  }


  /**
   * Returns the light of a full-strength lightning strike: sharp, dark shadows falling away from
   * the strike, blazing tile tops and nearly black terrace faces, and no sunlight.
   *
   * @param angle the direction the shadows fall, in radians counterclockwise from east (the
   *     strike is on the opposite side)
   * @return the strike's light
   */
  public static TerrainLight lightningStrike(double angle) {
    return lightningStrike(angle, 1);
  }

  /**
   * Returns the light of a lightning strike of a given strength. A weaker strike glows the tops
   * less, darkens the faces less and casts paler shadows; its shadows are just as long.
   *
   * @param angle the direction the shadows fall, in radians counterclockwise from east (the
   *     strike is on the opposite side)
   * @param intensity how strong the strike is, from 0 (none) to 1 (full)
   * @return the strike's light
   * @throws IllegalArgumentException if {@code intensity} is out of range
   */
  public static TerrainLight lightningStrike(double angle, double intensity) {
    checkRange("intensity", intensity, 1);
    return new TerrainLight(
        Math.cos(angle) * STRIKE_SHADOW_PER_LEVEL_PX,
        Math.sin(angle) * STRIKE_SHADOW_PER_LEVEL_PX,
        0,
        STRIKE_SHADOW_STRENGTH * intensity,
        false,
        STRIKE_TOP_GLOW * intensity,
        STRIKE_FACE_DARKENING * intensity);
  }

  /**
   * Returns this light seen through cloud: sunlight and shadows weakened by how much the clouds
   * block. Shadows keep their direction.
   *
   * @param cloudCover how much of the light the clouds block, from 0 (none) to 1 (all)
   * @return the dimmed light
   * @throws IllegalArgumentException if {@code cloudCover} is out of range
   */
  public TerrainLight overcast(double cloudCover) {
    checkRange("cloudCover", cloudCover, 1);
    double through = 1 - cloudCover;
    return new TerrainLight(
        dxPerLevel,
        dyPerLevel,
        sunlight * through,
        shadowStrength * through,
        softShadows,
        topGlow,
        faceDarkening,
        golden * through);
  }

  /**
   * Returns whether anything casts a shadow in this light.
   *
   * @return {@code false} if shadows have no strength or no length
   */
  public boolean castsShadows() {
    return shadowStrength > 0 && (dxPerLevel != 0 || dyPerLevel != 0);
  }

  /**
   * Returns how much sunlight a tile edge catches: none when it faces away from the light or there
   * is no sunlight, rising smoothly as it turns toward the light, scaled by how strong sunlight is.
   *
   * @param edge an edge of a tile's top
   * @return from 0 (in shade) to 1 (facing the full sun)
   */
  public double edgeLight(Edge edge) {
    if (sunlight <= 0) {
      return 0;
    }
    return sunlight * facing(edge);
  }

  /**
   * Returns how strongly golden hour gilds a tile edge: as much as the light's {@link #golden} for
   * an edge facing straight at the sun, less as it turns away, none facing away. Edges facing down
   * the screen ({@link Edge#FRONT}) always catch a little of it, {@link #FRONT_GLOW_SHARE} as much
   * as an edge facing the sun, whichever way the sun is. Unlike {@link #edgeLight} it does not fade
   * with the sunlight, so edges facing a setting sun glow bright orange-yellow even as the light
   * itself weakens.
   *
   * @param edge an edge of a tile's top
   * @return from 0 (no glow) to {@link #golden}
   */
  public double edgeGlow(Edge edge) {
    if (golden <= 0) {
      return 0;
    }
    double glow = golden * facing(edge);
    if (edge == Edge.FRONT) {
      glow = Math.max(glow, golden * FRONT_GLOW_SHARE);
    }
    return glow;
  }

  /** Returns how directly an edge faces the light, from 0 (away or edge-on) to 1, smoothly. */
  private double facing(Edge edge) {
    double length = Math.hypot(dxPerLevel, dyPerLevel);
    if (length == 0) {
      return 0;
    }
    // The light is in the opposite direction to the shadows.
    double towardLightX = -dxPerLevel / length;
    double towardLightY = -dyPerLevel / length;
    double facing;
    switch (edge) {
      case BACK:
        facing = towardLightY;
        break;
      case FRONT:
        facing = -towardLightY;
        break;
      case LEFT:
        facing = -towardLightX;
        break;
      case RIGHT:
      default:
        facing = towardLightX;
        break;
    }
    double t = Math.max(0, Math.min(1, facing / FULL_LIGHT_FACING));
    // Smoothstep, so the change is gentle at both ends.
    return t * t * (3 - 2 * t);
  }

  private static void checkRange(String name, double value, double max) {
    if (!(value >= 0 && value <= max)) {
      throw new IllegalArgumentException(name + " must be in [0, " + max + "]: " + value);
    }
  }
}
