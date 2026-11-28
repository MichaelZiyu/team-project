package game.interactor.gameModel.weather;

import game.interactor.gameModel.Texture;
import game.interactor.outputData.RenderableData.RenderableItem;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * The weather over the whole screen, as a layer for the renderer: falling rain or snow that
 * drifts toward the vanishing point ({@link RainPerspective}), splashes or resting flakes where it
 * lands, a darker or hazier sky, and lightning in thunderstorms.
 *
 * <p>Rain is lit by the sun, like the bright rims on the terrain: in full daylight streaks are a
 * pale, bright blue, and as the sun sets they fade to a dim blue-grey. The game passes in how
 * strong sunlight is with {@link #setSunlight}; it should be the same value a rim facing straight
 * at the sun is lit by, which is the sun's daylight strength. A lightning flash lights the rain
 * up too, so it shows in a storm at night. Snow is lit the same way, from near white by day to a
 * dim grey-blue at night.
 *
 * <p>Snowflakes are little pixel shapes ({@link SnowflakeShape}) drawn with square pixels that grow
 * from {@link #MIN_FLAKE_PIXEL_PX} far below the camera to {@link #MAX_FLAKE_PIXEL_PX} close to
 * it, keeping the game's pixel-art look.
 *
 * <p>Lightning brightens the whole scene for a split second rather than covering it in white: the
 * storm's darkening lifts, so the terrain shows at full brightness with all its detail, and a cool
 * light is added on top that the terrain still shows through.
 *
 * <p>Splashes land on the tile tops of the ground ({@link #setGround}), and are left out where a
 * character stands ({@link #setSplashBlockers}): rain falls in front of characters, but its
 * splashes are on the ground behind them.
 *
 * <p>{@link #update} moves the weather on in time, once a game tick. {@link #paintFrame} paints it,
 * for one frame, into a screen-sized see-through image, to be shown fixed to the screen on top of
 * the world. The renderer draws a frame on its own thread, so an image handed over must not be
 * painted again while it may still be drawn: the layer takes turns between {@link #FRAME_IMAGES}
 * images, so the one it paints is always one the renderer has finished with.
 */
public final class WeatherLayer {

  /** Told about each lightning strike as it begins, for example to sound its thunder. */
  @FunctionalInterface
  public interface LightningListener {
    /**
     * Receives a strike that has just begun.
     *
     * @param intensity its overall strength, from three quarters to 1 (full)
     */
    void struck(double intensity);
  }

  /** The sky's darkening during rain: a slight blue at about 18%. */
  static final Color RAIN_TINT = new Color(42, 62, 112, 45);

  /** The sky's darkening during a thunderstorm: a deep blue-grey at about 37%. */
  static final Color STORM_TINT = new Color(18, 22, 38, 95);

  /** The sky during snow: a soft grey-blue haze at about 14%. */
  static final Color SNOW_TINT = new Color(150, 162, 185, 35);

  /** The sky during a blizzard: a pale white-out at about 33%. */
  static final Color BLIZZARD_TINT = new Color(215, 222, 235, 85);

  /** The colour of snowflakes in full sunlight. */
  static final Color SNOW_COLOUR = new Color(240, 246, 255);

  /** The colour of snowflakes with no sunlight at all, at night. */
  static final Color SNOW_COLOUR_DARK = new Color(95, 104, 128);

  /** The size of a snowflake's pixels far below the camera, in screen pixels. */
  static final int MIN_FLAKE_PIXEL_PX = 2;

  /** The size of a snowflake's pixels close to the camera, in screen pixels. */
  static final int MAX_FLAKE_PIXEL_PX = 5;

  /** The colour of rain streaks in full sunlight, before their see-through-ness. */
  static final Color RAIN_COLOUR = new Color(190, 210, 235);

  /** The colour of rain streaks with no sunlight at all, at night. */
  static final Color RAIN_COLOUR_DARK = new Color(70, 82, 105);

  /** How wide the streak of a drop near the ground is, in pixels. */
  static final double MIN_STREAK_WIDTH_PX = 0.9;

  /** How wide the streak of a drop near the camera is, in pixels. */
  static final double MAX_STREAK_WIDTH_PX = 1.8;

  /** How much the light on the rain must change before its colours are worked out again. */
  private static final double LIGHT_STEP = 1.0 / 64;

  /**
   * The cool light a lightning flash adds over the scene at its brightest, at about 40%, on top of
   * lifting the storm's darkening; the terrain still shows clearly through it.
   */
  static final Color LIGHTNING_COLOUR = new Color(230, 236, 255, 102);

  /** How fast the sky's darkening fades in or out, as a share of the change per second. */
  static final double TINT_FADE_PER_SECOND = 0.6;

  /** How many steps of see-through-ness streaks are drawn with; keeps painting fast. */
  private static final int SHADES = 8;

  /**
   * How many images the layer takes turns painting into. The game hands over a frame each tick and
   * the renderer draws the latest one, so by the time an image comes round again its frame is two
   * frames old, and long finished with.
   */
  static final int FRAME_IMAGES = 3;

  private final int width;
  private final int height;
  private final RainPerspective perspective;
  private final RainField rain;
  private final SnowField snow;
  private final LightningFlash lightning;
  private final BufferedImage[] images = new BufferedImage[FRAME_IMAGES];
  private int nextImage;
  private BufferedImage lastImage;
  private final Color[] streakShades = new Color[SHADES];
  private final Color[] flakeShades = new Color[SHADES];
  private final Stroke[] streakStrokes = new Stroke[SHADES];

  private Weather weather = Weather.CLEAR;
  private double fadingCloudCover;
  private boolean terrainLightning;
  private RainStyle baseRain = RainStyle.NONE;
  private SnowStyle baseSnow = SnowStyle.NONE;
  private double rainAmount = 1;
  private double rainSpeed = 1;
  private double sunlight = 1;
  private double shadedLight = Double.NaN;
  private double tintAmount;
  private Color tintTarget = RAIN_TINT;
  private double tintTargetAmount;
  private List<RenderableItem> splashBlockers = List.of();
  private LightningListener lightningListener = intensity -> {};
  private int heardStrikes;
  private long cameraX;
  private long cameraY;

  /**
   * Creates a clear sky for a screen of the given size.
   *
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   * @param seed seeds where rain falls and when lightning strikes
   */
  public WeatherLayer(int viewWidthPx, int viewHeightPx, long seed) {
    this.width = viewWidthPx;
    this.height = viewHeightPx;
    this.perspective = new RainPerspective(viewWidthPx, viewHeightPx);
    this.rain = new RainField(perspective, viewWidthPx, viewHeightPx, seed);
    this.snow = new SnowField(perspective, viewWidthPx, viewHeightPx, seed + 2);
    this.lightning = new LightningFlash(seed + 1);
    for (int i = 0; i < FRAME_IMAGES; i++) {
      images[i] = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }
    for (int i = 0; i < SHADES; i++) {
      double nearness = i / (SHADES - 1.0);
      // A little thinner far away, thickening gently toward the camera.
      double widthPx =
          MIN_STREAK_WIDTH_PX + (MAX_STREAK_WIDTH_PX - MIN_STREAK_WIDTH_PX) * nearness;
      streakStrokes[i] =
          new BasicStroke((float) widthPx, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    }
  }

  /**
   * Changes the weather. Rain, snow and the sky's darkening change gradually.
   *
   * @param weather the new weather
   */
  public void setWeather(Weather weather) {
    this.weather = weather;
    baseRain = RainStyle.NONE;
    baseSnow = SnowStyle.NONE;
    tintTargetAmount = 1;
    if (weather != Weather.CLEAR) {
      // Clearing up keeps the old cloud cover, so it fades away with the sky's darkening.
      fadingCloudCover = weather.cloudCover();
    }
    switch (weather) {
      case RAIN:
        baseRain = RainStyle.RAIN;
        tintTarget = RAIN_TINT;
        break;
      case THUNDERSTORM:
        baseRain = RainStyle.THUNDERSTORM;
        tintTarget = STORM_TINT;
        break;
      case SNOW:
        baseSnow = SnowStyle.SNOW;
        tintTarget = SNOW_TINT;
        break;
      case BLIZZARD:
        baseSnow = SnowStyle.BLIZZARD;
        tintTarget = BLIZZARD_TINT;
        break;
      case CLEAR:
      default:
        tintTargetAmount = 0;
        break;
    }
    applyRain();
    lightning.setActive(weather == Weather.THUNDERSTORM);
  }

  /**
   * Sets how strongly the sun lights the rain.
   *
   * @param light from 0 (night: dim rain) to 1 (full daylight: bright rain); pass the sun's
   *     daylight strength, which is what a terrain rim facing straight at the sun is lit by
   * @throws IllegalArgumentException if {@code light} is out of range
   */
  public void setSunlight(double light) {
    if (!(light >= 0 && light <= 1)) {
      throw new IllegalArgumentException("light must be in [0, 1]: " + light);
    }
    this.sunlight = light;
  }

  /**
   * Returns how strongly the sun lights the rain.
   *
   * @return from 0 (night) to 1 (full daylight)
   */
  public double getSunlight() {
    return sunlight;
  }

  /**
   * Gives the rain and snow the ground to land on, so splashes sit on raised tile tops instead of
   * the faces of blocks.
   *
   * @param ground the ground, or {@code null} for flat ground
   */
  public void setGround(Ground ground) {
    rain.setGround(ground);
    snow.setGround(ground);
  }

  /**
   * Sets the characters standing in front of the ground: no splash is drawn where one of them is.
   *
   * @param blockers each character as the renderer draws it: its image, its middle and its bottom
   *     edge, in world pixels; one with no image covers nothing
   */
  public void setSplashBlockers(List<RenderableItem> blockers) {
    this.splashBlockers = List.copyOf(blockers);
  }

  /**
   * Makes rain or snow heavier or lighter than usual for its weather. The change is gradual.
   *
   * @param amount how many drops, compared with usual: 0 is none, 1 usual
   * @throws IllegalArgumentException if {@code amount} is negative
   */
  public void setRainAmount(double amount) {
    if (!(amount >= 0)) {
      throw new IllegalArgumentException("amount must not be negative: " + amount);
    }
    rainAmount = amount;
    applyRain();
  }

  /**
   * Returns how heavy rain or snow is compared with usual for its weather.
   *
   * @return 1 for usual
   */
  public double getRainAmount() {
    return rainAmount;
  }

  /**
   * Makes rain or snow fall faster or slower than usual for its weather; faster rain also leaves
   * longer streaks.
   *
   * @param speed how fast drops or flakes fall, compared with usual: 1 is usual
   * @throws IllegalArgumentException if {@code speed} is not positive
   */
  public void setRainSpeed(double speed) {
    if (!(speed > 0)) {
      throw new IllegalArgumentException("speed must be positive: " + speed);
    }
    rainSpeed = speed;
    applyRain();
  }

  /**
   * Returns how fast rain or snow falls compared with usual for its weather.
   *
   * @return 1 for usual
   */
  public double getRainSpeed() {
    return rainSpeed;
  }

  /** Sets the rain and snow to the weather's usual ones, adjusted by the amount and speed. */
  private void applyRain() {
    rain.setStyle(baseRain.scaled(rainAmount, rainSpeed));
    snow.setStyle(baseSnow.scaled(rainAmount, rainSpeed));
  }

  /**
   * Returns the current weather.
   *
   * @return the weather last set
   */
  public Weather getWeather() {
    return weather;
  }

  /**
   * Returns the perspective rain is drawn with, for example to move the vanishing point.
   *
   * @return the perspective
   */
  public RainPerspective perspective() {
    return perspective;
  }

  /** Makes lightning strike right now, if the weather is anything (mostly for testing). */
  public void strikeLightning() {
    lightning.strikeNow();
  }

  /**
   * Makes the next lightning strike due now (mostly for testing); like any strike, it waits until
   * {@link #setStrikeReady ready}.
   */
  public void strikeLightningSoon() {
    lightning.strikeSoon();
  }

  /**
   * Says whether a due lightning strike may go ahead, for example once the terrain lit by it has
   * been painted.
   *
   * @param ready {@code true} once everything the strike needs is prepared
   */
  public void setStrikeReady(boolean ready) {
    lightning.setReady(ready);
  }

  /**
   * Returns the direction of the next lightning strike, chosen in advance so its light can be
   * prepared.
   *
   * @return the direction its shadows will fall, in radians counterclockwise from east
   */
  public double upcomingStrikeAngle() {
    return lightning.upcomingAngle();
  }

  /**
   * Returns how strong the next lightning strike will be, chosen in advance so its light can be
   * prepared.
   *
   * @return from three quarters to 1 (full)
   */
  public double upcomingStrikeIntensity() {
    return lightning.upcomingIntensity();
  }

  /**
   * Returns how long until the next lightning strike is due.
   *
   * @return seconds; 0 if due now, infinite if there is no thunderstorm
   */
  public double secondsUntilStrike() {
    return lightning.secondsUntilStrike();
  }

  /**
   * Sets who is told about each lightning strike as it begins, such as the thunder.
   *
   * @param listener told each strike's overall strength; replaces any set before
   */
  public void setLightningListener(LightningListener listener) {
    this.lightningListener = listener;
  }

  /**
   * Returns whether this is one of a lightning flash's bright moments: the moments to show the
   * terrain lit by the strike.
   *
   * @return {@code true} during the brightest part of a strong stroke
   */
  public boolean isStrikeBright() {
    return lightning.isBright();
  }

  /**
   * Says whether the terrain itself shows lightning strikes. If so, the white wash is left out
   * during a flash's bright moments, so it does not cover the terrain's own strike lighting; it
   * still shows as each stroke fades.
   *
   * @param shown {@code true} if the terrain shows strikes
   */
  public void setTerrainLightning(boolean shown) {
    this.terrainLightning = shown;
  }

  /**
   * Returns how much of the sun the weather's clouds block right now, fading in and out with the
   * weather; for dimming the terrain's sunlight.
   *
   * @return from 0 (clear) to 1 (no sun gets through)
   */
  public double cloudCover() {
    return fadingCloudCover * tintAmount;
  }

  /**
   * Moves the weather forward in time, around the camera: rain and snow fall, lightning comes and
   * goes, and the sky's darkening fades in or out. Nothing is painted; see {@link #paintFrame}.
   *
   * @param seconds how much time passed since the last update, in seconds
   * @param cameraX the camera's x position, in world pixels; weather falls around it
   * @param cameraY the camera's y position, in world pixels
   */
  public void update(double seconds, double cameraX, double cameraY) {
    follow(cameraX, cameraY);
    rain.update(seconds, this.cameraX, this.cameraY);
    snow.update(seconds, this.cameraX, this.cameraY);
    lightning.update(seconds);
    if (lightning.strikeCount() != heardStrikes) {
      heardStrikes = lightning.strikeCount();
      lightningListener.struck(lightning.strikeIntensity());
    }
    double step = TINT_FADE_PER_SECOND * seconds;
    tintAmount += Math.max(-step, Math.min(step, tintTargetAmount - tintAmount));
  }

  /**
   * Returns whether there is any weather to show: rain or snow in the air or landed, a darkened
   * sky, or a lightning flash.
   *
   * @return {@code false} for a clear sky, when {@link #paintFrame} has nothing to paint
   */
  public boolean isShowing() {
    return !rain.isEmpty() || !snow.isEmpty() || tintAmount > 0 || lightning.brightness() > 0;
  }

  /**
   * Paints the weather as seen from the camera, for one frame, into a screen-sized see-through
   * image, to be drawn fixed to the screen with its top-left corner at the screen's.
   *
   * @param cameraX the camera's x position, in world pixels: the middle of the screen
   * @param cameraY the camera's y position, in world pixels
   * @return the painted image, never painted again until {@link #FRAME_IMAGES} more frames have
   *     been; {@code null} if there is no weather to show
   */
  public BufferedImage paintFrame(double cameraX, double cameraY) {
    follow(cameraX, cameraY);
    if (!isShowing()) {
      return null;
    }
    BufferedImage image = images[nextImage];
    nextImage = (nextImage + 1) % FRAME_IMAGES;
    paint(image);
    lastImage = image;
    return image;
  }

  /** Rounds the camera to whole pixels, as the renderer does, so splashes meet the ground. */
  private void follow(double cameraX, double cameraY) {
    this.cameraX = Math.round(cameraX);
    this.cameraY = Math.round(cameraY);
  }

  /**
   * Returns how many raindrops are falling right now.
   *
   * @return the number of drops in the air
   */
  int dropCount() {
    return rain.dropCount();
  }

  /**
   * Returns how many snowflakes are falling right now.
   *
   * @return the number of flakes in the air
   */
  int flakeCount() {
    return snow.flakeCount();
  }

  /**
   * Returns the image the layer last painted.
   *
   * @return the screen-sized image; {@code null} before the first
   */
  BufferedImage image() {
    return lastImage;
  }

  /**
   * Returns the colour of the rain's brightest streak right now.
   *
   * @return blended from {@link #RAIN_COLOUR_DARK} to {@link #RAIN_COLOUR} by the light
   */
  Color brightestStreak() {
    shadeStreaks();
    return streakShades[SHADES - 1];
  }

  /**
   * Works out the streak and snowflake colours for the light on them right now: sunlight, or a
   * lightning flash if that is brighter. Blends from the dark colour to the bright one, as terrain
   * rims blend from their dark image to their bright one.
   */
  private void shadeStreaks() {
    double light = Math.max(sunlight, lightning.brightness());
    if (Math.abs(light - shadedLight) < LIGHT_STEP) {
      return;
    }
    shadedLight = light;
    int red = mix(RAIN_COLOUR_DARK.getRed(), RAIN_COLOUR.getRed(), light);
    int green = mix(RAIN_COLOUR_DARK.getGreen(), RAIN_COLOUR.getGreen(), light);
    int blue = mix(RAIN_COLOUR_DARK.getBlue(), RAIN_COLOUR.getBlue(), light);
    for (int i = 0; i < SHADES; i++) {
      double nearness = i / (SHADES - 1.0);
      // Faint far away, brightening steeply toward the camera.
      int alpha = (int) Math.round(255 * (0.1 + 0.75 * Math.pow(nearness, 1.5)));
      streakShades[i] = new Color(red, green, blue, alpha);
      // Flakes are solid: a little see-through far away, fully opaque near the camera.
      flakeShades[i] =
          new Color(
              mix(SNOW_COLOUR_DARK.getRed(), SNOW_COLOUR.getRed(), light),
              mix(SNOW_COLOUR_DARK.getGreen(), SNOW_COLOUR.getGreen(), light),
              mix(SNOW_COLOUR_DARK.getBlue(), SNOW_COLOUR.getBlue(), light),
              (int) Math.round(255 * (0.55 + 0.45 * nearness)));
    }
  }

  /**
   * Returns the colour of the nearest snowflakes right now.
   *
   * @return blended from {@link #SNOW_COLOUR_DARK} to {@link #SNOW_COLOUR} by the light
   */
  Color nearestFlake() {
    shadeStreaks();
    return flakeShades[SHADES - 1];
  }

  /**
   * Returns how big a snowflake's pixels are drawn.
   *
   * @param nearness how close it is to the camera, from 0 (at the ground) to 1 (near)
   * @return the size, in whole screen pixels
   */
  static int flakePixel(double nearness) {
    return (int) Math.round(
        MIN_FLAKE_PIXEL_PX + (MAX_FLAKE_PIXEL_PX - MIN_FLAKE_PIXEL_PX) * nearness);
  }

  /** Draws a snowflake centred on a point, with square pixels of the given size. */
  private static void drawFlake(
      Graphics2D g, double x, double y, int pixel, SnowflakeShape shape) {
    int size = SnowflakeShape.GRID * pixel;
    // Whole pixels, so the flake stays crisp.
    int left = (int) Math.round(x - size / 2.0);
    int top = (int) Math.round(y - size / 2.0);
    for (int row = 0; row < SnowflakeShape.GRID; row++) {
      for (int column = 0; column < SnowflakeShape.GRID; column++) {
        if (shape.isFilled(column, row)) {
          g.fillRect(left + column * pixel, top + row * pixel, pixel, pixel);
        }
      }
    }
  }

  private static int mix(int from, int to, double amount) {
    return (int) Math.round(from + (to - from) * amount);
  }

  /**
   * Paints the darker sky, the rain and its splashes, the snow and its resting flakes, and any
   * lightning into an image.
   */
  private void paint(BufferedImage image) {
    shadeStreaks();
    Graphics2D g = image.createGraphics();
    try {
      // Replace last frame with the sky's darkening (fully see-through if there is none). A
      // lightning flash lifts the darkening, so the whole scene brightens for a moment.
      double flash = lightning.brightness();
      g.setComposite(AlphaComposite.Src);
      g.setColor(scaleAlpha(tintTarget, tintAmount * (1 - flash)));
      g.fillRect(0, 0, width, height);
      g.setComposite(AlphaComposite.SrcOver);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      Line2D.Double line = new Line2D.Double();
      rain.forEachStreak(
          (headX, headY, tailX, tailY, nearness) -> {
            int shade = (int) Math.round(nearness * (SHADES - 1));
            g.setColor(streakShades[shade]);
            g.setStroke(streakStrokes[shade]);
            line.setLine(tailX, tailY, headX, headY);
            g.draw(line);
          });
      g.setStroke(new BasicStroke(1f));
      Ellipse2D.Double ring = new Ellipse2D.Double();
      rain.forEachSplash(
          (x, y, progress) -> {
            if (isBlocked(x, y)) {
              return;
            }
            double ringWidth = 3 + 11 * progress;
            double ringHeight = ringWidth * 0.55;
            g.setColor(scaleAlpha(streakShades[SHADES - 1], 0.95 * (1 - progress)));
            ring.setFrame(x - ringWidth / 2, y - ringHeight / 2, ringWidth, ringHeight);
            g.draw(ring);
          });
      // Snow is drawn without smoothing, so its square pixels stay sharp. Resting flakes first,
      // on the ground under the falling ones.
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
      snow.forEachResting(
          (x, y, progress, shape) -> {
            if (isBlocked(x, y)) {
              return;
            }
            g.setColor(scaleAlpha(flakeShades[0], 1 - progress));
            drawFlake(g, x, y, MIN_FLAKE_PIXEL_PX, shape);
          });
      snow.forEachFlake(
          (x, y, nearness, shape) -> {
            g.setColor(flakeShades[(int) Math.round(nearness * (SHADES - 1))]);
            drawFlake(g, x, y, flakePixel(nearness), shape);
          });
      boolean terrainShowsStrike = terrainLightning && lightning.isBright();
      if (flash > 0 && !terrainShowsStrike) {
        g.setColor(scaleAlpha(LIGHTNING_COLOUR, flash));
        g.fillRect(0, 0, width, height);
      }
    } finally {
      g.dispose();
    }
  }

  /**
   * Returns whether a point on screen is covered by one of the splash blockers, placed as the
   * renderer places them. A blocker with no image is not drawn, so it covers nothing.
   */
  boolean isBlocked(double screenX, double screenY) {
    for (RenderableItem blocker : splashBlockers) {
      Texture image = blocker.texture();
      if (image == null) {
        continue;
      }
      int blockerWidth = image.getWidth();
      int blockerHeight = image.getHeight();
      double left = blocker.x() - cameraX + width / 2 - blockerWidth / 2;
      double top = height / 2 - (blocker.y() - cameraY) - blockerHeight;
      if (screenX >= left
          && screenX <= left + blockerWidth
          && screenY >= top
          && screenY <= top + blockerHeight) {
        return true;
      }
    }
    return false;
  }

  /** Returns a colour with its opacity multiplied by {@code amount}, from 0 to 1. */
  private static Color scaleAlpha(Color colour, double amount) {
    int alpha = (int) Math.round(colour.getAlpha() * Math.max(0, Math.min(1, amount)));
    return new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), alpha);
  }
}
