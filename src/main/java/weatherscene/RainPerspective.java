package weatherscene;

/**
 * Where something above the ground appears on screen, for a camera looking down at the world.
 *
 * <p>The ground is drawn flat, as the game's camera draws it. Something above the ground is
 * closer to the camera, so perspective pushes it away from the <em>vanishing point</em>: the spot
 * on screen that is straight below the camera. A raindrop falling straight down therefore slides
 * toward the vanishing point, and lands exactly on the ground position it was falling onto. The
 * vanishing point is always centred left to right. By default it sits just off the bottom of the
 * screen, which gives the look of a camera tilted forward, as in most top-down games; it can be
 * moved anywhere up or down.
 *
 * <p>The vanishing point can also be widened into a <em>vanishing oval</em>, twice as wide as it is
 * tall. Each drop then heads toward its own point of the oval: the further from the centre it
 * falls, the further out on the oval it heads, so rain at the edges of the screen spreads less and
 * lands further from the centre. In effect the perspective is weaker across, and a little weaker
 * up and down; with no oval it is the same in both directions.
 */
public final class RainPerspective {

  /**
   * How far below the centre of the screen the vanishing point is by default, as a share of the
   * screen's height: just off the bottom edge.
   */
  public static final double DEFAULT_VANISHING_OFFSET = 0.96;

  /** How wide the vanishing oval is by default, as a share of the screen's width. */
  public static final double DEFAULT_OVAL_WIDTH = 0.45;

  /**
   * The furthest the vanishing point can be from the centre of the screen, up or down, as a share
   * of the screen's height. Beyond 0.5 it is off the screen.
   */
  public static final double MAX_VANISHING_OFFSET = 3;

  /** How high the camera is above the ground, in pixels; sets how strong the perspective is. */
  public static final double CAMERA_HEIGHT_PX = 900;

  /** How tall the vanishing oval is compared with its width. */
  public static final double OVAL_SQUASH = 0.5;

  /** The widest the vanishing oval can be, as a share of the screen's width. */
  public static final double MAX_OVAL_WIDTH = 0.9;

  private final int viewWidthPx;
  private final int viewHeightPx;
  private double vanishingOffset = DEFAULT_VANISHING_OFFSET;
  private double ovalWidth = DEFAULT_OVAL_WIDTH;

  /**
   * Creates the perspective for a screen of the given size.
   *
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   */
  public RainPerspective(int viewWidthPx, int viewHeightPx) {
    this.viewWidthPx = viewWidthPx;
    this.viewHeightPx = viewHeightPx;
  }

  /**
   * Moves the vanishing point up or down.
   *
   * @param offset how far below the centre of the screen it is, as a share of the screen's height:
   *     0 is the centre, 0.5 the bottom edge, and more is off the screen; negative is above the
   *     centre. At most {@link #MAX_VANISHING_OFFSET} either way
   * @throws IllegalArgumentException if the offset is out of range
   */
  public void setVanishingOffset(double offset) {
    if (!(Math.abs(offset) <= MAX_VANISHING_OFFSET)) {
      throw new IllegalArgumentException(
          "offset must be within " + MAX_VANISHING_OFFSET + " of 0: " + offset);
    }
    this.vanishingOffset = offset;
  }

  /**
   * Returns how far below the centre of the screen the vanishing point is.
   *
   * @return a share of the screen's height
   */
  public double getVanishingOffset() {
    return vanishingOffset;
  }

  /**
   * Widens the vanishing point into an oval, or shrinks it back to a point.
   *
   * @param widthShare the oval's width as a share of the screen's width: 0 is a single point, at
   *     most {@link #MAX_OVAL_WIDTH}; its height is {@link #OVAL_SQUASH} times its width
   * @throws IllegalArgumentException if the width is out of range
   */
  public void setOvalWidth(double widthShare) {
    if (!(widthShare >= 0 && widthShare <= MAX_OVAL_WIDTH)) {
      throw new IllegalArgumentException(
          "oval width must be in [0, " + MAX_OVAL_WIDTH + "]: " + widthShare);
    }
    this.ovalWidth = widthShare;
  }

  /**
   * Returns the vanishing oval's width.
   *
   * @return a share of the screen's width; 0 for a single point
   */
  public double getOvalWidth() {
    return ovalWidth;
  }

  /**
   * Returns the vanishing oval's half-width.
   *
   * @return pixels from its centre to its left or right edge
   */
  public double ovalRadiusX() {
    return ovalWidth * viewWidthPx / 2.0;
  }

  /**
   * Returns the vanishing oval's half-height.
   *
   * @return pixels from its centre to its top or bottom edge
   */
  public double ovalRadiusY() {
    return ovalRadiusX() * OVAL_SQUASH;
  }

  /**
   * Returns the vanishing point's x position on screen.
   *
   * @return pixels from the left edge
   */
  public double vanishingX() {
    return viewWidthPx / 2.0;
  }

  /**
   * Returns the vanishing point's y position on screen.
   *
   * @return pixels from the top edge
   */
  public double vanishingY() {
    return viewHeightPx / 2.0 + vanishingOffset * viewHeightPx;
  }

  /**
   * Returns how much bigger, and how much further from the vanishing point, something appears at
   * a height above the ground.
   *
   * @param heightPx the height above the ground, in pixels; less than {@link #CAMERA_HEIGHT_PX}
   * @return 1 on the ground, growing toward the camera
   */
  public double scale(double heightPx) {
    return CAMERA_HEIGHT_PX / (CAMERA_HEIGHT_PX - heightPx);
  }

  /**
   * Returns where a point on the ground appears on screen, using the game camera's rule: the
   * camera is centred on the player, and the world's y axis points up while the screen's points
   * down.
   *
   * @param worldX the point's x position in the world, in pixels
   * @param worldY the point's y position in the world, in pixels
   * @param cameraX the player's x position, in pixels
   * @param cameraY the player's y position, in pixels
   * @return {@code {x, y}} on screen, in pixels
   */
  public double[] groundToScreen(double worldX, double worldY, double cameraX, double cameraY) {
    return new double[] {
      worldX - cameraX + viewWidthPx / 2.0, viewHeightPx / 2.0 - (worldY - cameraY)
    };
  }

  /**
   * Returns how far something at a height appears from the vanishing oval's centre, left to right,
   * compared with the ground below it.
   *
   * @param heightPx the height above the ground, in pixels; less than {@link #CAMERA_HEIGHT_PX}
   * @return 1 on the ground, growing toward the camera; equal to {@link #scale} with no oval
   */
  public double spreadX(double heightPx) {
    return spread(ovalRadiusX() / (viewWidthPx / 2.0), heightPx);
  }

  /**
   * Returns how far something at a height appears from the vanishing oval's centre, up and down,
   * compared with the ground below it.
   *
   * @param heightPx the height above the ground, in pixels; less than {@link #CAMERA_HEIGHT_PX}
   * @return 1 on the ground, growing toward the camera; equal to {@link #scale} with no oval
   */
  public double spreadY(double heightPx) {
    return spread(ovalRadiusY() / (viewHeightPx / 2.0), heightPx);
  }

  /**
   * Returns the height at which something appears {@code spread} times as far from the vanishing
   * oval's centre, left to right, as the ground below it: the reverse of {@link #spreadX}.
   *
   * @param spread the spread wanted; 1 or less means the ground itself
   * @return the height, in pixels; 0 for a spread of 1 or less, approaching {@link
   *     #CAMERA_HEIGHT_PX} as the spread grows
   */
  public double heightForSpreadX(double spread) {
    return heightForSpread(ovalRadiusX() / (viewWidthPx / 2.0), spread);
  }

  /**
   * Returns the height at which something appears {@code spread} times as far from the vanishing
   * oval's centre, up and down, as the ground below it: the reverse of {@link #spreadY}.
   *
   * @param spread the spread wanted; 1 or less means the ground itself
   * @return the height, in pixels; 0 for a spread of 1 or less
   */
  public double heightForSpreadY(double spread) {
    return heightForSpread(ovalRadiusY() / (viewHeightPx / 2.0), spread);
  }

  /** Solves {@code spread = share + (1 - share) * C / (C - h)} for the height {@code h}. */
  private static double heightForSpread(double share, double spread) {
    if (spread <= 1) {
      return 0;
    }
    return CAMERA_HEIGHT_PX * (1 - (1 - share) / (spread - share));
  }

  /**
   * Returns the spread along one axis. A drop whose ground is a share {@code u} of the half-screen
   * from the centre heads for the oval point the same share out, so it appears at {@code oval +
   * (ground - oval) * scale}; measured from the centre that is {@code ground * (share + (1 -
   * share) * scale)}, where {@code share} is the oval's half-size over the half-screen.
   */
  private double spread(double share, double heightPx) {
    return share + (1 - share) * scale(heightPx);
  }

  /**
   * Returns the point on the ground that something at a height appears in front of: the reverse of
   * {@link #project}.
   *
   * @param screenX where it appears on screen, x, in pixels
   * @param screenY where it appears on screen, y, in pixels
   * @param heightPx its height above the ground, in pixels
   * @return {@code {x, y}} on screen of the ground directly below it
   */
  public double[] unproject(double screenX, double screenY, double heightPx) {
    double vx = vanishingX();
    double vy = vanishingY();
    return new double[] {
      vx + (screenX - vx) / spreadX(heightPx), vy + (screenY - vy) / spreadY(heightPx)
    };
  }

  /**
   * Returns where a point at a height above the ground appears on screen.
   *
   * @param groundScreenX where the ground below it appears, x, in pixels
   * @param groundScreenY where the ground below it appears, y, in pixels
   * @param heightPx its height above the ground, in pixels
   * @return {@code {x, y}} on screen: pushed away from the vanishing oval's centre by {@link
   *     #spreadX} and {@link #spreadY}
   */
  public double[] project(double groundScreenX, double groundScreenY, double heightPx) {
    double vx = vanishingX();
    double vy = vanishingY();
    return new double[] {
      vx + (groundScreenX - vx) * spreadX(heightPx), vy + (groundScreenY - vy) * spreadY(heightPx)
    };
  }
}
