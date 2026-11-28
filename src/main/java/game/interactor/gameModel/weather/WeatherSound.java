package game.interactor.gameModel.weather;

import game.interactor.outputData.RenderableData.LoopingSound;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The sound of the weather: gentle rain while it rains, heavy rain in a thunderstorm.
 *
 * <p>Each sound loops for as long as its weather lasts, fading in over {@link #FADE_SECONDS} when
 * the weather starts and out again when it ends, so changing from rain to a thunderstorm blends
 * one into the other. Other weather is silent. Call {@link #setWeather} when the weather changes
 * and {@link #update} every tick.
 *
 * <p>Nothing is played here: {@link #loops} describes what should be heard, for each frame the game
 * hands to the renderer, and the game's audio plays it.
 */
public final class WeatherSound {

  /** The light rain loop, named by its file under the game's sound folder. */
  public static final String LIGHT_RAIN = "weather/light-rain";

  /** The heavy rain loop, named by its file under the game's sound folder. */
  public static final String HEAVY_RAIN = "weather/heavy-rain";

  /** How long a sound takes to fade fully in or out, in seconds. */
  static final double FADE_SECONDS = 2.5;

  private double lightLevel;
  private double heavyLevel;
  private double lightTarget;
  private double heavyTarget;

  /**
   * Changes the weather the sound follows. The sounds fade over the next updates.
   *
   * @param weather the new weather
   */
  public void setWeather(Weather weather) {
    lightTarget = weather == Weather.RAIN ? 1 : 0;
    heavyTarget = weather == Weather.THUNDERSTORM ? 1 : 0;
  }

  /**
   * Moves the fades forward in time.
   *
   * @param seconds how much time passed since the last update, in seconds
   */
  public void update(double seconds) {
    double step = seconds / FADE_SECONDS;
    lightLevel = toward(lightLevel, lightTarget, step);
    heavyLevel = toward(heavyLevel, heavyTarget, step);
  }

  /**
   * Returns the looping sounds that should be heard right now, at their volumes.
   *
   * @return each rain sound that is not silent; empty in dry weather once the rain has faded
   */
  public List<LoopingSound> loops() {
    List<LoopingSound> loops = new ArrayList<>();
    if (lightLevel > 0) {
      loops.add(new LoopingSound(LIGHT_RAIN, lightLevel));
    }
    if (heavyLevel > 0) {
      loops.add(new LoopingSound(HEAVY_RAIN, heavyLevel));
    }
    return Collections.unmodifiableList(loops);
  }

  /**
   * Returns how loud the light rain is right now.
   *
   * @return from 0 (silent) to 1 (full)
   */
  public double lightRainLevel() {
    return lightLevel;
  }

  /**
   * Returns how loud the heavy rain is right now.
   *
   * @return from 0 (silent) to 1 (full)
   */
  public double heavyRainLevel() {
    return heavyLevel;
  }

  private static double toward(double level, double target, double step) {
    return level + Math.max(-step, Math.min(step, target - level));
  }
}
