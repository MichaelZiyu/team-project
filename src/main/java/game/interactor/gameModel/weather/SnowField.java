package game.interactor.gameModel.weather;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * The snowflakes in the air around the player, and the ones that have just landed.
 *
 * <p>Like rain ({@link RainField}), each flake has a spot on the ground it is falling toward and a
 * height above it, and seen through {@link RainPerspective} it drifts toward the vanishing point
 * as it falls. Where flakes fall and start follows {@link FallingArea}. Unlike rain, flakes fall
 * slowly, each at its own pace, and sway from side to side as they go. Half are plus-shaped and
 * half X-shaped ({@link SnowflakeShape}). Flakes the wind blows off one side come back in on the
 * other ({@link FallingArea#wrapAcross}), so even a blizzard stays evenly spread.
 *
 * <p>A flake that lands rests on the tile top it fell on for {@link #REST_SECONDS}, fading away,
 * unless a taller tile in front hides it. Changing the {@link SnowStyle} starts or stops snow
 * gradually: new flakes arrive from above over {@link #RAMP_SECONDS}, and when snow stops the
 * flakes already falling finish their fall.
 *
 * <p>This class only simulates; {@link WeatherLayer} draws it.
 */
public final class SnowField {

  /** How long a landed flake stays visible, fading away, in seconds. */
  static final double REST_SECONDS = 1.2;

  /** How long snow takes to build up to full strength after it starts, in seconds. */
  static final double RAMP_SECONDS = 3;

  /** How much slower or faster than the style's speed a flake can fall, as a share. */
  static final double SPEED_SPREAD = 0.3;

  /** The slowest a flake sways from side to side, in full swings per second. */
  static final double MIN_SWAY_PER_SECOND = 0.25;

  /** The fastest a flake sways from side to side, in full swings per second. */
  static final double MAX_SWAY_PER_SECOND = 0.6;

  /**
   * The height at which a flake counts as fully near the camera, for drawing: flakes this high or
   * higher are drawn biggest and brightest.
   */
  static final double NEAR_HEIGHT_PX = 0.85 * RainPerspective.CAMERA_HEIGHT_PX;

  /** Receives one flake, on screen. */
  @FunctionalInterface
  public interface FlakeConsumer {
    /**
     * Receives a flake.
     *
     * @param x the flake's centre on screen, x
     * @param y the flake's centre on screen, y
     * @param nearness how close to the camera it is, from 0 (at the ground) to 1 (near)
     * @param shape its shape
     */
    void accept(double x, double y, double nearness, SnowflakeShape shape);
  }

  /** Receives one landed flake, on screen. */
  @FunctionalInterface
  public interface RestingConsumer {
    /**
     * Receives a landed flake.
     *
     * @param x its centre on screen, x
     * @param y its centre on screen, y
     * @param progress how far through its rest it is, from 0 (just landed) to 1 (gone)
     * @param shape its shape
     */
    void accept(double x, double y, double progress, SnowflakeShape shape);
  }

  /** One flake in the air. */
  private static final class Flake {
    private double groundX;
    private double groundY;
    private double height;
    private double speed;
    private double swayPhase;
    private double swayRate;
    private SnowflakeShape shape;

    /** Returns how far east of its spot the flake has swayed, in pixels. */
    double sway(double swayPx) {
      return swayPx * Math.sin(swayPhase);
    }
  }

  /** A landed flake: where, how far up the screen its tile top is drawn, and how long ago. */
  private static final class Resting {
    private final double groundX;
    private final double groundY;
    private final int liftPx;
    private final SnowflakeShape shape;
    private double age;

    Resting(double groundX, double groundY, int liftPx, SnowflakeShape shape) {
      this.groundX = groundX;
      this.groundY = groundY;
      this.liftPx = liftPx;
      this.shape = shape;
    }
  }

  private final RainPerspective perspective;
  private final FallingArea area;
  private final int viewWidthPx;
  private final int viewHeightPx;
  private final Random random;
  private final List<Flake> flakes = new ArrayList<>();
  private final List<Resting> resting = new ArrayList<>();

  private SnowStyle style = SnowStyle.NONE;
  private double spawnBudget;
  private double cameraX;
  private double cameraY;

  /**
   * Creates an empty sky.
   *
   * @param perspective how heights appear on screen
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   * @param seed seeds where flakes fall, so tests can repeat a snowfall exactly
   */
  public SnowField(RainPerspective perspective, int viewWidthPx, int viewHeightPx, long seed) {
    this.perspective = perspective;
    this.area = new FallingArea(perspective, viewWidthPx, viewHeightPx);
    this.viewWidthPx = viewWidthPx;
    this.viewHeightPx = viewHeightPx;
    this.random = new Random(seed);
  }

  /**
   * Changes how heavily snow falls. The change is gradual; see the class description.
   *
   * @param style the new snow
   */
  public void setStyle(SnowStyle style) {
    this.style = style;
  }

  /**
   * Returns how heavily snow is falling.
   *
   * @return the current style
   */
  public SnowStyle getStyle() {
    return style;
  }

  /**
   * Gives the ground to land on, so things land on raised tile tops, and ones hidden behind taller
   * tiles are left out.
   *
   * @param ground the ground, or {@code null} for flat ground
   */
  public void setGround(Ground ground) {
    area.setGround(ground);
  }

  /**
   * Moves time forward: flakes fall, sway and drift, landed ones fade, and new ones start as
   * needed.
   *
   * @param seconds how much time passed, in seconds
   * @param cameraX the player's x position, in pixels; snow falls around it
   * @param cameraY the player's y position, in pixels
   */
  public void update(double seconds, double cameraX, double cameraY) {
    this.cameraX = cameraX;
    this.cameraY = cameraY;
    Iterator<Resting> landed = resting.iterator();
    while (landed.hasNext()) {
      Resting flake = landed.next();
      flake.age += seconds;
      if (flake.age >= REST_SECONDS) {
        landed.remove();
      }
    }
    Iterator<Flake> falling = flakes.iterator();
    while (falling.hasNext()) {
      Flake flake = falling.next();
      flake.height -= style.fallSpeedPx() * flake.speed * seconds;
      flake.groundX = area.wrapAcross(flake.groundX + style.windEastPx() * seconds, cameraX);
      flake.groundY += style.windNorthPx() * seconds;
      flake.swayPhase += flake.swayRate * seconds;
      double landX = flake.groundX + flake.sway(style.swayPx());
      int liftPx = area.liftAt(landX, flake.groundY);
      boolean landedNow = flake.height <= liftPx;
      if (landedNow && !area.hiddenInFront(landX, flake.groundY, liftPx)) {
        resting.add(new Resting(landX, flake.groundY, liftPx, flake.shape));
      }
      if (landedNow || !area.covers(flake.groundX, flake.groundY, cameraX, cameraY)) {
        if (flakes.size() > area.scaledCount(style.flakes())) {
          falling.remove();
        } else {
          restartAtTop(flake);
        }
      }
    }
    addNewFlakes(seconds);
  }

  /** Starts new flakes, building up to the target number over {@link #RAMP_SECONDS}. */
  private void addNewFlakes(double seconds) {
    int target = area.scaledCount(style.flakes());
    int missing = target - flakes.size();
    if (missing <= 0) {
      spawnBudget = 0;
      return;
    }
    spawnBudget += target * seconds / RAMP_SECONDS;
    int count = (int) Math.min(missing, Math.floor(spawnBudget));
    spawnBudget -= count;
    for (int i = 0; i < count; i++) {
      Flake flake = new Flake();
      restartAtTop(flake);
      flakes.add(flake);
    }
  }

  /** Puts a flake over a random spot, just high enough to be out of view, with a new character. */
  private void restartAtTop(Flake flake) {
    double[] start = area.pickStart(random, cameraX, cameraY);
    flake.groundX = start[0];
    flake.groundY = start[1];
    flake.height = start[2];
    flake.speed = 1 + SPEED_SPREAD * (2 * random.nextDouble() - 1);
    flake.swayPhase = random.nextDouble() * 2 * Math.PI;
    double swingsPerSecond =
        MIN_SWAY_PER_SECOND + random.nextDouble() * (MAX_SWAY_PER_SECOND - MIN_SWAY_PER_SECOND);
    flake.swayRate = 2 * Math.PI * swingsPerSecond;
    flake.shape = random.nextBoolean() ? SnowflakeShape.PLUS : SnowflakeShape.CROSS;
  }

  /**
   * Gives every flake in the air that can be seen, as it appears on screen right now, including a
   * border of {@link FallingArea#MARGIN_PX} around the screen.
   *
   * @param consumer receives each flake
   */
  public void forEachFlake(FlakeConsumer consumer) {
    double vx = perspective.vanishingX();
    double vy = perspective.vanishingY();
    double shareX = perspective.ovalShareX();
    double shareY = perspective.ovalShareY();
    // Written out rather than calling RainPerspective for each flake, since this runs for
    // thousands of them every frame.
    double offsetX = viewWidthPx / 2.0 - cameraX;
    double offsetY = viewHeightPx / 2.0 + cameraY;
    for (Flake flake : flakes) {
      double groundX = flake.groundX + flake.sway(style.swayPx()) + offsetX;
      double groundY = offsetY - flake.groundY;
      // As for rain (see RainField#forEachStreak), the flake heads for its own point of the
      // vanishing oval worked out from where the wind will have carried it when it lands, so
      // the wind alone does not bend its path; only its sway does.
      double secondsLeft = flake.height / (style.fallSpeedPx() * flake.speed);
      double ownX = vx + shareX * (groundX + style.windEastPx() * secondsLeft - vx);
      double ownY = vy + shareY * (groundY - style.windNorthPx() * secondsLeft - vy);
      double scale = perspective.scale(flake.height);
      double x = ownX + (groundX - ownX) * scale;
      double y = ownY + (groundY - ownY) * scale;
      // Drawn out into a border around the screen, so the edge of the snow never shows.
      double border = FallingArea.MARGIN_PX;
      if (x < -border || x > viewWidthPx + border || y < -border || y > viewHeightPx + border) {
        continue;
      }
      consumer.accept(x, y, Math.min(1, flake.height / NEAR_HEIGHT_PX), flake.shape);
    }
  }

  /**
   * Gives every landed flake, as it appears on screen right now.
   *
   * @param consumer receives each landed flake
   */
  public void forEachResting(RestingConsumer consumer) {
    for (Resting flake : resting) {
      double[] at = perspective.groundToScreen(flake.groundX, flake.groundY, cameraX, cameraY);
      consumer.accept(at[0], at[1] - flake.liftPx, flake.age / REST_SECONDS, flake.shape);
    }
  }

  /**
   * Returns how many flakes are in the air, including any falling outside the screen's view.
   *
   * @return the number of falling flakes
   */
  public int flakeCount() {
    return flakes.size();
  }

  /**
   * Returns how many landed flakes are showing.
   *
   * @return the number of landed flakes
   */
  public int restingCount() {
    return resting.size();
  }

  /**
   * Returns whether there is anything to draw.
   *
   * @return {@code true} while any flake, falling or landed, remains
   */
  public boolean isEmpty() {
    return flakes.isEmpty() && resting.isEmpty();
  }
}
