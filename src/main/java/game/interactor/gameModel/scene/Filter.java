package game.interactor.gameModel.scene;

import game.interactor.World;
import game.interactor.gameModel.Texture;
import game.interactor.gameModel.daynight.DayClock;
import game.interactor.gameModel.daynight.SkyTint;
import game.interactor.gameModel.daynight.Sun;
import game.interactor.gameModel.weather.Ground;
import game.interactor.gameModel.weather.ThunderSound;
import game.interactor.gameModel.weather.Weather;
import game.interactor.gameModel.weather.WeatherLayer;
import game.interactor.gameModel.weather.WeatherSound;
import game.interactor.outputData.RenderableData.Layer;
import game.interactor.outputData.RenderableData.LoopingSound;
import game.interactor.outputData.RenderableData.RenderableItem;
import game.interactor.outputData.RenderableData.SoundEvent;
import game.settings.GameSettings;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Everything laid over the whole view: the time of day and the weather.
 *
 * <p>The filter keeps the game's clock (a day lasts {@link #DAY_SECONDS}), the sun that moves with
 * it, and the weather. Each {@link #tick} moves them on by one tick. For each frame it gives a
 * layer fixed to the screen ({@link #layer}): the sky's tint for the time of day (warm at dawn and
 * dusk, a dim blue at night), with the rain or snow over it; and what should be heard ({@link
 * #loopingSounds}, {@link #soundEvents}): rain, and thunder after lightning.
 *
 * <p>Other parts of the game can read how the sky lights the world: {@link #sun} for where the sun
 * is and how much daylight there is, and {@link #cloudCover} for how much the weather's clouds dim
 * it, for example to light the terrain.
 */
public class Filter extends Scene {

  /** How long a whole day lasts, in seconds of play. */
  public static final double DAY_SECONDS = 600;

  /** The view size used until {@link #setViewSize} is called, in pixels. */
  static final int DEFAULT_VIEW_WIDTH = 800;

  /** The view size used until {@link #setViewSize} is called, in pixels. */
  static final int DEFAULT_VIEW_HEIGHT = 600;

  /**
   * How much any part of the sky's tint (red, green, blue or opacity) must change before a new
   * image is made for it; the tint changes slowly, so most frames reuse the same image.
   */
  static final int TINT_STEP = 2;

  private final DayClock clock;
  private final Sun sun;
  private final long seed;
  private final WeatherSound weatherSound = new WeatherSound();
  private final ThunderSound thunder;
  private WeatherLayer weather;
  private Weather currentWeather = Weather.CLEAR;

  /** Weather asked for from another thread, applied at the next tick; null if none is waiting. */
  private final AtomicReference<Weather> requestedWeather = new AtomicReference<>();
  private Ground ground = Ground.FLAT;
  private int viewWidth = DEFAULT_VIEW_WIDTH;
  private int viewHeight = DEFAULT_VIEW_HEIGHT;
  private double cameraX;
  private double cameraY;
  private Color tintColour;
  private Texture tintTexture;

  /** Creates the filter at the start of a day's morning, in clear weather. */
  public Filter() {
    this(new Random().nextLong());
  }

  /**
   * Creates the filter with repeatable weather; used by tests.
   *
   * @param seed seeds where rain falls and when lightning strikes
   */
  Filter(long seed) {
    this.seed = seed;
    this.clock = DayClock.forRealTime(DAY_SECONDS, 1.0 / GameSettings.FPS);
    this.sun = new Sun(clock);
    this.thunder = new ThunderSound(seed + 3);
    this.weather = newWeatherLayer();
  }

  /**
   * Moves the time of day and the weather on by one tick ({@code 1 / GameSettings.FPS} seconds).
   *
   * @param world the world
   */
  @Override
  public void tick(World world) {
    Weather requested = requestedWeather.getAndSet(null);
    if (requested != null && requested != currentWeather) {
      setWeather(requested);
    }
    double seconds = 1.0 / GameSettings.FPS;
    clock.tick();
    weather.setSunlight(sun.daylight());
    weather.update(seconds, cameraX, cameraY);
    weatherSound.update(seconds);
    thunder.update(seconds);
  }

  /**
   * Returns the layer to draw over the whole view for a frame: the sky's tint, then the weather
   * over it, both fixed to the screen. Remembers the camera, so the weather keeps falling around
   * it.
   *
   * @param cameraX the frame's camera x position, in world pixels
   * @param cameraY the frame's camera y position, in world pixels
   * @param characters the characters as the frame draws them, so rain splashes are not drawn on
   *     them
   * @return the layer; empty when there is nothing to lay over the view
   */
  public Layer layer(double cameraX, double cameraY, List<RenderableItem> characters) {
    this.cameraX = cameraX;
    this.cameraY = cameraY;
    List<RenderableItem> items = new ArrayList<>();
    Texture tint = tint();
    if (tint != null) {
      items.add(new RenderableItem(tint, 0, 0));
    }
    weather.setSplashBlockers(characters);
    BufferedImage painted = weather.paintFrame(cameraX, cameraY);
    if (painted != null) {
      items.add(
          new RenderableItem(new Texture(painted, painted.getWidth(), painted.getHeight()), 0, 0));
    }
    return Layer.onScreen(items);
  }

  /**
   * Returns the looping sounds to be heard now: rain.
   *
   * @return the sounds, at their volumes
   */
  public List<LoopingSound> loopingSounds() {
    return weatherSound.loops();
  }

  /**
   * Returns the sounds played once in the last moment: thunder.
   *
   * @return the sound events
   */
  public List<SoundEvent> soundEvents() {
    return thunder.events();
  }

  /**
   * Changes the weather. Rain, snow, the sky's darkening and the sound of rain change gradually.
   *
   * @param weather the new weather
   */
  public void setWeather(Weather weather) {
    this.currentWeather = weather;
    this.weather.setWeather(weather);
    weatherSound.setWeather(weather);
  }

  /**
   * Asks for the weather to change at the next tick. Unlike {@link #setWeather}, this is safe to
   * call from any thread, such as one waiting on a weather service; if it is called again before
   * the tick, the later weather wins.
   *
   * @param weather the new weather
   */
  public void requestWeather(Weather weather) {
    requestedWeather.set(Objects.requireNonNull(weather, "weather"));
  }

  /**
   * Returns the weather.
   *
   * @return the weather last set
   */
  public Weather getWeather() {
    return currentWeather;
  }

  /**
   * Returns the sun, which moves with the time of day.
   *
   * @return the sun
   */
  public Sun sun() {
    return sun;
  }

  /**
   * Returns the game's clock.
   *
   * @return the clock
   */
  public DayClock clock() {
    return clock;
  }

  /**
   * Returns how much of the sun the weather's clouds block right now, fading in and out with the
   * weather.
   *
   * @return from 0 (clear) to 1 (no sun gets through)
   */
  public double cloudCover() {
    return weather.cloudCover();
  }

  /**
   * Gives rain and snow the ground to land on.
   *
   * @param ground the ground, or {@code null} for flat ground
   */
  public void setGround(Ground ground) {
    this.ground = ground == null ? Ground.FLAT : ground;
    weather.setGround(this.ground);
  }

  /**
   * Sets the size of the view the layer covers. The weather starts afresh for the new size.
   *
   * @param widthPx the view's width, in pixels
   * @param heightPx the view's height, in pixels
   */
  public void setViewSize(int widthPx, int heightPx) {
    this.viewWidth = widthPx;
    this.viewHeight = heightPx;
    this.weather = newWeatherLayer();
    this.weather.setWeather(currentWeather);
    this.weather.setGround(ground);
    this.tintColour = null;
    this.tintTexture = null;
  }

  /**
   * Returns the weather, to change how it looks while testing (such as the rain's amount).
   *
   * @return the weather layer
   */
  public WeatherLayer weatherLayer() {
    return weather;
  }

  private WeatherLayer newWeatherLayer() {
    WeatherLayer layer = new WeatherLayer(viewWidth, viewHeight, seed);
    layer.setLightningListener(thunder::strike);
    return layer;
  }

  /**
   * Returns the sky's tint as a screen-sized image, or {@code null} in the middle of the day when
   * there is none. A new image is made only when the colour has changed enough to see; an image
   * once handed over is never painted again.
   */
  private Texture tint() {
    Color colour = SkyTint.at(clock.timeOfDay());
    if (colour.getAlpha() == 0) {
      tintColour = null;
      tintTexture = null;
      return null;
    }
    if (tintTexture == null || farApart(colour, tintColour)) {
      BufferedImage image = new BufferedImage(viewWidth, viewHeight, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = image.createGraphics();
      try {
        g.setColor(colour);
        g.fillRect(0, 0, viewWidth, viewHeight);
      } finally {
        g.dispose();
      }
      tintColour = colour;
      tintTexture = new Texture(image, viewWidth, viewHeight);
    }
    return tintTexture;
  }

  private static boolean farApart(Color a, Color b) {
    return Math.abs(a.getRed() - b.getRed()) >= TINT_STEP
        || Math.abs(a.getGreen() - b.getGreen()) >= TINT_STEP
        || Math.abs(a.getBlue() - b.getBlue()) >= TINT_STEP
        || Math.abs(a.getAlpha() - b.getAlpha()) >= TINT_STEP;
  }
}
