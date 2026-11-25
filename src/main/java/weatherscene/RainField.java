package weatherscene;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import terrain.Terrain;

/**
 * The raindrops in the air around the player, and the splashes where they land.
 *
 * <p>Each drop has a spot on the ground it is falling toward, in world coordinates, and a height
 * above it. Every update it falls (and drifts with the wind); seen through {@link
 * RainPerspective} it slides toward the vanishing point and lands exactly on its spot, leaving a
 * splash. Where drops fall, and where they start, follows {@link FallingArea}: evenly over the
 * screen, streaming in from its edges. Landed drops, and drops the player walks away from, start
 * again, so rain keeps falling around the player. Changing the {@link RainStyle} starts or stops
 * rain gradually: new drops arrive from above over {@link #RAMP_SECONDS}, and when rain stops the
 * drops already falling finish their fall.
 *
 * <p>Given the terrain ({@link #setTerrain}), drops land on the tops of raised tiles, and their
 * splashes sit on those tops as the game draws them, lifted up the screen; a splash on a spot
 * hidden behind a taller tile in front is not shown. Without terrain the ground is flat.
 *
 * <p>This class only simulates; {@link WeatherLayer} draws it.
 */
public final class RainField {

  /**
   * The highest a drop can be, in pixels: almost at the camera. Drops start lower than this, just
   * high enough to be out of view; only drops falling almost under the vanishing point need it.
   */
  static final double MAX_HEIGHT_PX = FallingArea.MAX_HEIGHT_PX;

  /**
   * The height at which a drop counts as fully near the camera, for drawing: drops this high or
   * higher are drawn boldest, and lower ones fainter and thinner.
   */
  static final double NEAR_HEIGHT_PX = 0.85 * RainPerspective.CAMERA_HEIGHT_PX;

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

  /**
   * Where a drop landed, in world pixels; how far up the screen the tile top it landed on is
   * lifted, in pixels; and how long ago.
   */
  private static final class Splash {
    private final double groundX;
    private final double groundY;
    private final int liftPx;
    private double age;

    Splash(double groundX, double groundY, int liftPx) {
      this.groundX = groundX;
      this.groundY = groundY;
      this.liftPx = liftPx;
    }
  }

  private final RainPerspective perspective;
  private final FallingArea area;
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
    this.area = new FallingArea(perspective, viewWidthPx, viewHeightPx);
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
      int liftPx = area.liftAt(drop.groundX, drop.groundY);
      if (drop.height <= liftPx) {
        if (!area.hiddenInFront(drop.groundX, drop.groundY, liftPx)) {
          splashes.add(new Splash(drop.groundX, drop.groundY, liftPx));
        }
        if (drops.size() > targetDrops()) {
          falling.remove();
        } else {
          restartAtTop(drop);
        }
      } else if (!area.covers(drop.groundX, drop.groundY, cameraX, cameraY)) {
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
    return area.scaledCount(style.drops());
  }

  /** Puts a drop over a random spot of the rain area, just high enough to be out of view. */
  private void restartAtTop(Drop drop) {
    double[] start = area.pickStart(random, cameraX, cameraY);
    drop.groundX = start[0];
    drop.groundY = start[1];
    drop.height = start[2];
  }

  /**
   * Returns how high a drop falling onto the given ground must start to appear just beyond the
   * edge of the screen; see {@link FallingArea#startHeight}.
   *
   * @param groundScreenX where the ground appears on screen, x, in pixels
   * @param groundScreenY where the ground appears on screen, y, in pixels
   * @return the starting height, in pixels
   */
  double startHeight(double groundScreenX, double groundScreenY) {
    return area.startHeight(groundScreenX, groundScreenY);
  }

  /**
   * Gives the streak of every drop that can be seen, as it appears on screen right now, including
   * a border of {@link FallingArea#MARGIN_PX} around the screen. Drops whose streak lies wholly
   * outside that are skipped.
   *
   * @param consumer receives each streak
   */
  public void forEachStreak(StreakConsumer consumer) {
    double vx = perspective.vanishingX();
    double vy = perspective.vanishingY();
    double shareX = perspective.ovalShareX();
    double shareY = perspective.ovalShareY();
    double driftX = style.windEastPx() * STREAK_SECONDS;
    double driftY = style.windNorthPx() * STREAK_SECONDS;
    double fall = style.fallSpeedPx() * STREAK_SECONDS;
    // How far the wind carries a drop for each pixel it still has to fall.
    double windEastPerPx = style.windEastPx() / style.fallSpeedPx();
    double windNorthPerPx = style.windNorthPx() / style.fallSpeedPx();
    // Written out rather than calling RainPerspective for each point: this runs for thousands of
    // drops every frame.
    double offsetX = viewWidthPx / 2.0 - cameraX;
    double offsetY = viewHeightPx / 2.0 + cameraY;
    for (Drop drop : drops) {
      // The ground below the drop, on screen.
      double groundX = drop.groundX + offsetX;
      double groundY = offsetY - drop.groundY;
      // The drop heads for its own point of the vanishing oval, worked out from where it will
      // land rather than where it is over now: the wind moves the latter as it falls, which
      // would bend its path. This way the path is straight, as a camera sees real rain.
      double ownX = vx + shareX * (groundX + windEastPerPx * drop.height - vx);
      double ownY = vy + shareY * (groundY - windNorthPerPx * drop.height - vy);
      double scale = perspective.scale(drop.height);
      double headX = ownX + (groundX - ownX) * scale;
      double headY = ownY + (groundY - ownY) * scale;
      // Where the drop was a moment ago: higher, and before the wind moved it.
      double before = perspective.scale(Math.min(MAX_HEIGHT_PX, drop.height + fall));
      double tailX = ownX + (groundX - driftX - ownX) * before;
      double tailY = ownY + (groundY + driftY - ownY) * before;
      double length = Math.hypot(tailX - headX, tailY - headY);
      if (length > MAX_STREAK_PX) {
        tailX = headX + (tailX - headX) * MAX_STREAK_PX / length;
        tailY = headY + (tailY - headY) * MAX_STREAK_PX / length;
      }
      // Drawn out into a border around the screen, so the edge of the rain never shows.
      double border = FallingArea.MARGIN_PX;
      if (Math.max(headX, tailX) < -border
          || Math.min(headX, tailX) > viewWidthPx + border
          || Math.max(headY, tailY) < -border
          || Math.min(headY, tailY) > viewHeightPx + border) {
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
      consumer.accept(at[0], at[1] - splash.liftPx, splash.age / SPLASH_SECONDS);
    }
  }

  /**
   * Gives the rain the terrain to land on, so drops land on raised tile tops and splashes hidden
   * behind taller tiles are left out.
   *
   * @param terrain the terrain under the rain, or {@code null} for flat ground
   */
  public void setTerrain(Terrain terrain) {
    area.setTerrain(terrain);
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
