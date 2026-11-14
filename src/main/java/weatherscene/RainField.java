package weatherscene;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * The raindrops in the air around the player, and the splashes where they land.
 *
 * <p>Each drop has a spot on the ground it is falling toward, in world coordinates, and a height
 * above it. Every update it falls (and drifts with the wind); seen through {@link
 * RainPerspective} it slides toward the vanishing point and lands exactly on its spot, leaving a
 * splash. Drops fall onto ground spread evenly over the area that can appear on screen; perspective
 * then spreads them evenly over the screen too, wherever the vanishing point is. When it is off
 * the screen, that area also takes in the ground between the screen and the vanishing point, so
 * more drops are simulated to keep the same amount of rain in view. Each drop starts just high
 * enough to appear beyond the edge of the screen, so rain streams in from the edges rather than
 * popping up in the middle. Landed drops, and drops the player walks away from, start again, so
 * rain keeps falling around the player. Changing the
 * {@link RainStyle} starts or stops rain gradually: new drops arrive from above over {@link
 * #RAMP_SECONDS}, and when rain stops the drops already falling finish their fall.
 *
 * <p>This class only simulates; {@link WeatherLayer} draws it.
 */
public final class RainField {

  /**
   * The highest a drop can be, in pixels: almost at the camera. Drops start lower than this, just
   * high enough to be out of view; only drops falling almost under the vanishing point need it.
   */
  static final double MAX_HEIGHT_PX = 0.99 * RainPerspective.CAMERA_HEIGHT_PX;

  /**
   * The height at which a drop counts as fully near the camera, for drawing: drops this high or
   * higher are drawn boldest, and lower ones fainter and thinner.
   */
  static final double NEAR_HEIGHT_PX = 0.85 * RainPerspective.CAMERA_HEIGHT_PX;

  /** How many times to pick a landing spot before accepting one that cannot start out of view. */
  private static final int SPOT_TRIES = 20;

  /** How far beyond the edge of the screen drops start, as a share of their distance out. */
  static final double START_BEYOND_EDGE = 0.03;

  /** How long a splash stays visible, in seconds. */
  static final double SPLASH_SECONDS = 0.3;

  /** How long rain takes to build up to full strength after it starts, in seconds. */
  static final double RAMP_SECONDS = 1.5;

  /** How long the streak behind each drop is, as seconds of its fall; a motion blur. */
  static final double STREAK_SECONDS = 0.025;

  /**
   * The longest a streak is drawn, in pixels. Drops near the camera move very fast on screen; this
   * keeps them as short dashes instead of long lines.
   */
  static final double MAX_STREAK_PX = 40;

  /** Extra space around the screen where drops may fall, so the edges never look empty. */
  static final double MARGIN_PX = 96;

  /** Receives one drop's streak, on screen. */
  @FunctionalInterface
  public interface StreakConsumer {
    /**
     * Receives a streak.
     *
     * @param headX the drop's position now, x
     * @param headY the drop's position now, y
     * @param tailX where it was a moment ago, x
     * @param tailY where it was a moment ago, y
     * @param nearness how close to the camera it is, from 0 (at the ground) to 1 (near)
     */
    void accept(double headX, double headY, double tailX, double tailY, double nearness);
  }

  /** Receives one splash, on screen. */
  @FunctionalInterface
  public interface SplashConsumer {
    /**
     * Receives a splash.
     *
     * @param x the splash's centre on screen, x
     * @param y the splash's centre on screen, y
     * @param progress how far through its life it is, from 0 (just landed) to 1 (gone)
     */
    void accept(double x, double y, double progress);
  }

  /** One raindrop: the ground spot it is over, in world pixels, and its height. */
  private static final class Drop {
    private double groundX;
    private double groundY;
    private double height;
  }

  /** Where a drop landed, in world pixels, and how long ago. */
  private static final class Splash {
    private final double groundX;
    private final double groundY;
    private double age;

    Splash(double groundX, double groundY) {
      this.groundX = groundX;
      this.groundY = groundY;
    }
  }

  private final RainPerspective perspective;
  private final int viewWidthPx;
  private final int viewHeightPx;
  private final Random random;
  private final List<Drop> drops = new ArrayList<>();
  private final List<Splash> splashes = new ArrayList<>();

  private RainStyle style = RainStyle.NONE;
  private double spawnBudget;
  private double cameraX;
  private double cameraY;

  /**
   * Creates an empty sky.
   *
   * @param perspective how heights appear on screen
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   * @param seed seeds where drops fall, so tests can repeat a shower exactly
   */
  public RainField(RainPerspective perspective, int viewWidthPx, int viewHeightPx, long seed) {
    this.perspective = perspective;
    this.viewWidthPx = viewWidthPx;
    this.viewHeightPx = viewHeightPx;
    this.random = new Random(seed);
  }

  /**
   * Changes how heavily rain falls. The change is gradual; see the class description.
   *
   * @param style the new rain
   */
  public void setStyle(RainStyle style) {
    this.style = style;
  }

  /**
   * Returns how heavily rain is falling.
   *
   * @return the current style
   */
  public RainStyle getStyle() {
    return style;
  }

  /**
   * Moves time forward: drops fall and drift, landed ones splash, and new ones start as needed.
   *
   * @param seconds how much time passed, in seconds
   * @param cameraX the player's x position, in pixels; rain falls around it
   * @param cameraY the player's y position, in pixels
   */
  public void update(double seconds, double cameraX, double cameraY) {
    this.cameraX = cameraX;
    this.cameraY = cameraY;
    Iterator<Splash> oldSplashes = splashes.iterator();
    while (oldSplashes.hasNext()) {
      Splash splash = oldSplashes.next();
      splash.age += seconds;
      if (splash.age >= SPLASH_SECONDS) {
        oldSplashes.remove();
      }
    }
    Iterator<Drop> falling = drops.iterator();
    while (falling.hasNext()) {
      Drop drop = falling.next();
      drop.height -= style.fallSpeedPx() * seconds;
      drop.groundX += style.windEastPx() * seconds;
      drop.groundY += style.windNorthPx() * seconds;
      if (drop.height <= 0) {
        splashes.add(new Splash(drop.groundX, drop.groundY));
        if (drops.size() > targetDrops()) {
          falling.remove();
        } else {
          restartAtTop(drop);
        }
      } else if (!overRainArea(drop)) {
        // The player walked away from it, or the wind blew it away: start it again, or drop it
        // if the rain is easing off.
        if (drops.size() > targetDrops()) {
          falling.remove();
        } else {
          restartAtTop(drop);
        }
      }
    }
    addNewDrops(seconds);
  }

  /** Starts new drops, building up to the target number over {@link #RAMP_SECONDS}. */
  private void addNewDrops(double seconds) {
    int target = targetDrops();
    int missing = target - drops.size();
    if (missing <= 0) {
      spawnBudget = 0;
      return;
    }
    spawnBudget += target * seconds / RAMP_SECONDS;
    int count = (int) Math.min(missing, Math.floor(spawnBudget));
    spawnBudget -= count;
    for (int i = 0; i < count; i++) {
      Drop drop = new Drop();
      restartAtTop(drop);
      drops.add(drop);
    }
  }

  /**
   * Returns how many drops to simulate: the style's number, scaled up by how much bigger the rain
   * area is than the screen, so the same amount of rain is in view.
   */
  private int targetDrops() {
    double[] area = rainArea();
    double areaSize = (area[2] - area[0]) * (area[3] - area[1]);
    double screenSize = (viewWidthPx + 2 * MARGIN_PX) * (viewHeightPx + 2 * MARGIN_PX);
    return (int) Math.round(style.drops() * areaSize / screenSize);
  }

  /**
   * Returns the ground, in screen coordinates, that anything up to {@link #MAX_HEIGHT_PX} high can
   * appear in front of the screen from, as {@code {left, top, right, bottom}}: the screen (with a
   * margin) together with the screen shrunk toward the vanishing point by the perspective at the
   * greatest height.
   */
  private double[] rainArea() {
    double left = -MARGIN_PX;
    double top = -MARGIN_PX;
    double right = viewWidthPx + MARGIN_PX;
    double bottom = viewHeightPx + MARGIN_PX;
    double[] shrunkTopLeft = perspective.unproject(left, top, MAX_HEIGHT_PX);
    double[] shrunkBottomRight = perspective.unproject(right, bottom, MAX_HEIGHT_PX);
    return new double[] {
      Math.min(left, shrunkTopLeft[0]),
      Math.min(top, shrunkTopLeft[1]),
      Math.max(right, shrunkBottomRight[0]),
      Math.max(bottom, shrunkBottomRight[1])
    };
  }

  /**
   * Puts a drop over a random spot of the rain area, just high enough to be out of view. Spots so
   * close under the vanishing point that a drop would be in view even almost at the camera are
   * picked again: such drops would pop up in the middle of the screen.
   */
  private void restartAtTop(Drop drop) {
    double[] area = rainArea();
    double screenX = 0;
    double screenY = 0;
    for (int tries = 0; tries < SPOT_TRIES; tries++) {
      screenX = area[0] + random.nextDouble() * (area[2] - area[0]);
      screenY = area[1] + random.nextDouble() * (area[3] - area[1]);
      drop.height = startHeight(screenX, screenY);
      if (drop.height < MAX_HEIGHT_PX || !onScreen(screenX, screenY, drop.height)) {
        break;
      }
    }
    // Screen to world: the reverse of RainPerspective.groundToScreen.
    drop.groundX = screenX - viewWidthPx / 2.0 + cameraX;
    drop.groundY = viewHeightPx / 2.0 - screenY + cameraY;
  }

  /**
   * Returns how high a drop falling onto the given ground must start to appear just beyond the
   * edge of the screen: the lowest height at which it is out of view across or up and down. Drops
   * falling almost under the vanishing point cannot get out of view; they start at {@link
   * #MAX_HEIGHT_PX}.
   *
   * @param groundScreenX where the ground appears on screen, x, in pixels
   * @param groundScreenY where the ground appears on screen, y, in pixels
   * @return the starting height, in pixels
   */
  double startHeight(double groundScreenX, double groundScreenY) {
    double fromCentreX = groundScreenX - perspective.vanishingX();
    double fromCentreY = groundScreenY - perspective.vanishingY();
    double edgeX = fromCentreX > 0 ? viewWidthPx + MARGIN_PX : -MARGIN_PX;
    double edgeY = fromCentreY > 0 ? viewHeightPx + MARGIN_PX : -MARGIN_PX;
    double height = MAX_HEIGHT_PX;
    if (fromCentreX != 0) {
      double spread = (edgeX - perspective.vanishingX()) / fromCentreX;
      if (spread > 0) {
        height = Math.min(height, perspective.heightForSpreadX(spread * (1 + START_BEYOND_EDGE)));
      }
    }
    if (fromCentreY != 0) {
      double spread = (edgeY - perspective.vanishingY()) / fromCentreY;
      if (spread > 0) {
        height = Math.min(height, perspective.heightForSpreadY(spread * (1 + START_BEYOND_EDGE)));
      }
    }
    return height;
  }

  /** Returns whether something at a height above the given ground appears on screen. */
  private boolean onScreen(double groundScreenX, double groundScreenY, double height) {
    double[] at = perspective.project(groundScreenX, groundScreenY, height);
    return at[0] >= 0 && at[0] <= viewWidthPx && at[1] >= 0 && at[1] <= viewHeightPx;
  }

  /** Returns whether a drop is still falling onto the rain area around the player. */
  private boolean overRainArea(Drop drop) {
    double[] area = rainArea();
    double[] ground = perspective.groundToScreen(drop.groundX, drop.groundY, cameraX, cameraY);
    return ground[0] >= area[0] - MARGIN_PX
        && ground[0] <= area[2] + MARGIN_PX
        && ground[1] >= area[1] - MARGIN_PX
        && ground[1] <= area[3] + MARGIN_PX;
  }

  /**
   * Gives the streak of every drop that can be seen, as it appears on screen right now. Drops
   * whose streak lies wholly off the screen are skipped.
   *
   * @param consumer receives each streak
   */
  public void forEachStreak(StreakConsumer consumer) {
    double vx = perspective.vanishingX();
    double vy = perspective.vanishingY();
    double driftX = style.windEastPx() * STREAK_SECONDS;
    double driftY = style.windNorthPx() * STREAK_SECONDS;
    double fall = style.fallSpeedPx() * STREAK_SECONDS;
    // Written out rather than calling RainPerspective for each point: this runs for thousands of
    // drops every frame. It is the same arithmetic as groundToScreen followed by project.
    double offsetX = viewWidthPx / 2.0 - cameraX;
    double offsetY = viewHeightPx / 2.0 + cameraY;
    for (Drop drop : drops) {
      double headX = vx + (drop.groundX + offsetX - vx) * perspective.spreadX(drop.height);
      double headY = vy + (offsetY - drop.groundY - vy) * perspective.spreadY(drop.height);
      // Where the drop was a moment ago: higher, and before the wind moved it.
      double before = Math.min(MAX_HEIGHT_PX, drop.height + fall);
      double tailX = vx + (drop.groundX - driftX + offsetX - vx) * perspective.spreadX(before);
      double tailY = vy + (offsetY - (drop.groundY - driftY) - vy) * perspective.spreadY(before);
      double length = Math.hypot(tailX - headX, tailY - headY);
      if (length > MAX_STREAK_PX) {
        tailX = headX + (tailX - headX) * MAX_STREAK_PX / length;
        tailY = headY + (tailY - headY) * MAX_STREAK_PX / length;
      }
      if (Math.max(headX, tailX) < 0
          || Math.min(headX, tailX) > viewWidthPx
          || Math.max(headY, tailY) < 0
          || Math.min(headY, tailY) > viewHeightPx) {
        continue;
      }
      consumer.accept(headX, headY, tailX, tailY, Math.min(1, drop.height / NEAR_HEIGHT_PX));
    }
  }

  /**
   * Gives every splash, as it appears on screen right now.
   *
   * @param consumer receives each splash
   */
  public void forEachSplash(SplashConsumer consumer) {
    for (Splash splash : splashes) {
      double[] at = perspective.groundToScreen(splash.groundX, splash.groundY, cameraX, cameraY);
      consumer.accept(at[0], at[1], splash.age / SPLASH_SECONDS);
    }
  }

  /**
   * Returns how many drops are in the air, including any falling outside the screen's view.
   *
   * @return the number of falling drops
   */
  public int dropCount() {
    return drops.size();
  }

  /**
   * Returns how many splashes are showing.
   *
   * @return the number of splashes
   */
  public int splashCount() {
    return splashes.size();
  }

  /**
   * Returns whether there is anything to draw.
   *
   * @return {@code true} while any drop or splash remains
   */
  public boolean isEmpty() {
    return drops.isEmpty() && splashes.isEmpty();
  }
}
