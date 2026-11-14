package weatherscene;

import game.gameModel.Renderable;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import jswing.Texture;
import terrainscene.ImageTexture;
import weather.Weather;

/**
 * The weather over the whole screen, as a layer for the renderer: falling rain that streams
 * toward the vanishing point ({@link RainPerspective}), splashes where it lands, a darker sky,
 * and lightning in thunderstorms.
 *
 * <p>Rain is lit by the sun, like the bright rims on the terrain: in full daylight streaks are a
 * pale, bright blue, and as the sun sets they fade to a dim blue-grey. The game passes in how
 * strong sunlight is with {@link #setSunlight}; it should be the same value a rim facing straight
 * at the sun is lit by, which is the sun's daylight strength. A lightning flash lights the rain
 * up too, so it shows in a storm at night.
 *
 * <p>Lightning brightens the whole scene for a split second rather than covering it in white: the
 * storm's darkening lifts, so the terrain shows at full brightness with all its detail, and a cool
 * light is added on top that the terrain still shows through.
 *
 * <p>Everything is painted into one screen-sized see-through image each frame, so the renderer
 * only has one sprite to place. Call {@link #update} on the Swing thread before each frame. The
 * layer must be drawn on top of the world, so it goes in front of the terrain and characters in
 * the render order. Snow and blizzards are not drawn yet; they show as clear weather for now.
 */
public final class WeatherLayer implements Iterable<Renderable> {

  /** The sky's darkening during rain: a slight blue at about 18%. */
  static final Color RAIN_TINT = new Color(42, 62, 112, 45);

  /** The sky's darkening during a thunderstorm: a deep blue-grey at about 37%. */
  static final Color STORM_TINT = new Color(18, 22, 38, 95);

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

  /** Extra pixels the overlay reaches past each screen edge, so rounding never leaves a gap. */
  private static final int OVERHANG_PX = 2;

  private final int width;
  private final int height;
  private final RainPerspective perspective;
  private final RainField rain;
  private final LightningFlash lightning;
  private final BufferedImage image;
  private final Color[] streakShades = new Color[SHADES];
  private final Stroke[] streakStrokes = new Stroke[SHADES];
  private final Overlay overlay = new Overlay();

  private Weather weather = Weather.CLEAR;
  private RainStyle baseRain = RainStyle.NONE;
  private double rainAmount = 1;
  private double rainSpeed = 1;
  private double sunlight = 1;
  private double shadedLight = Double.NaN;
  private double tintAmount;
  private Color tintTarget = RAIN_TINT;
  private double tintTargetAmount;
  private List<Renderable> contents = List.of();

  /**
   * Creates a clear sky for a screen of the given size.
   *
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   * @param seed seeds where rain falls and when lightning strikes
   */
  public WeatherLayer(int viewWidthPx, int viewHeightPx, long seed) {
    this.width = viewWidthPx + 2 * OVERHANG_PX;
    this.height = viewHeightPx + 2 * OVERHANG_PX;
    this.perspective = new RainPerspective(viewWidthPx, viewHeightPx);
    this.rain = new RainField(perspective, viewWidthPx, viewHeightPx, seed);
    this.lightning = new LightningFlash(seed + 1);
    this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
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
   * Changes the weather. Rain and the sky's darkening change gradually.
   *
   * @param weather the new weather
   */
  public void setWeather(Weather weather) {
    this.weather = weather;
    switch (weather) {
      case RAIN:
        baseRain = RainStyle.RAIN;
        tintTarget = RAIN_TINT;
        tintTargetAmount = 1;
        break;
      case THUNDERSTORM:
        baseRain = RainStyle.THUNDERSTORM;
        tintTarget = STORM_TINT;
        tintTargetAmount = 1;
        break;
      default:
        baseRain = RainStyle.NONE;
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
   * Makes rain heavier or lighter than usual for its weather. The change is gradual.
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
   * Returns how heavy rain is compared with usual for its weather.
   *
   * @return 1 for usual
   */
  public double getRainAmount() {
    return rainAmount;
  }

  /**
   * Makes rain fall faster or slower than usual for its weather; faster rain also leaves longer
   * streaks.
   *
   * @param speed how fast drops fall, compared with usual: 1 is usual
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
   * Returns how fast rain falls compared with usual for its weather.
   *
   * @return 1 for usual
   */
  public double getRainSpeed() {
    return rainSpeed;
  }

  /** Sets the rain to the weather's usual rain, adjusted by the amount and speed. */
  private void applyRain() {
    rain.setStyle(baseRain.scaled(rainAmount, rainSpeed));
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
   * Moves the weather forward in time and repaints it, centred on the player.
   *
   * @param seconds how much time passed since the last update, in seconds
   * @param playerX the player's x position, in pixels; the camera is centred on it
   * @param playerY the player's y position, in pixels
   */
  public void update(double seconds, double playerX, double playerY) {
    // The renderer places sprites relative to the player's whole-pixel position.
    int cameraX = (int) playerX;
    int cameraY = (int) playerY;
    rain.update(seconds, cameraX, cameraY);
    lightning.update(seconds);
    double step = TINT_FADE_PER_SECOND * seconds;
    tintAmount += Math.max(-step, Math.min(step, tintTargetAmount - tintAmount));
    if (rain.isEmpty() && tintAmount <= 0 && lightning.brightness() <= 0) {
      contents = List.of();
      return;
    }
    paint();
    overlay.centreX = cameraX;
    overlay.bottomY = cameraY - height / 2;
    contents = Collections.singletonList(overlay);
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
   * Returns the image the layer last painted.
   *
   * @return the screen-sized overlay image
   */
  BufferedImage image() {
    return image;
  }

  @Override
  public Iterator<Renderable> iterator() {
    return contents.iterator();
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
   * Works out the streak colours for the light on the rain right now: sunlight, or a lightning
   * flash if that is brighter. Blends from the dark colour to the bright one, as terrain rims
   * blend from their dark image to their bright one.
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
    }
  }

  private static int mix(int from, int to, double amount) {
    return (int) Math.round(from + (to - from) * amount);
  }

  /** Paints the darker sky, the rain, the splashes and any lightning into the overlay image. */
  private void paint() {
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
      g.translate(OVERHANG_PX, OVERHANG_PX);
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
            double ringWidth = 3 + 11 * progress;
            double ringHeight = ringWidth * 0.55;
            g.setColor(scaleAlpha(streakShades[SHADES - 1], 0.95 * (1 - progress)));
            ring.setFrame(x - ringWidth / 2, y - ringHeight / 2, ringWidth, ringHeight);
            g.draw(ring);
          });
      if (flash > 0) {
        g.setColor(scaleAlpha(LIGHTNING_COLOUR, flash));
        g.fillRect(-OVERHANG_PX, -OVERHANG_PX, width, height);
      }
    } finally {
      g.dispose();
    }
  }

  /** Returns a colour with its opacity multiplied by {@code amount}, from 0 to 1. */
  private static Color scaleAlpha(Color colour, double amount) {
    int alpha = (int) Math.round(colour.getAlpha() * Math.max(0, Math.min(1, amount)));
    return new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), alpha);
  }

  /** The screen-sized sprite showing the painted weather. */
  private final class Overlay implements Renderable {

    private static final long serialVersionUID = 1L;

    private int centreX;
    private int bottomY;

    /** Made on first use, on the Swing thread, and never saved. */
    private transient Texture texture;

    @Override
    public Texture getTexture() {
      if (texture == null) {
        texture = new ImageTexture(image);
      }
      return texture;
    }

    @Override
    public int getX() {
      return centreX;
    }

    @Override
    public int getY() {
      return bottomY;
    }

    @Override
    public int getWidth() {
      return width;
    }

    @Override
    public int getHeight() {
      return height;
    }
  }
}
