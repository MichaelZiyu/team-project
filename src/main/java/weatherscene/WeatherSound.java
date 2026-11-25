package weatherscene;

import weather.Weather;

/**
 * The sound of the weather: gentle rain while it rains, heavy rain in a thunderstorm.
 *
 * <p>Each sound loops for as long as its weather lasts, fading in over {@link #FADE_SECONDS} when
 * the weather starts and out again when it ends, so changing from rain to a thunderstorm blends
 * one into the other. Other weather is silent. Call {@link #setWeather} when the weather changes
 * and {@link #update} every frame, on the Swing thread like the rest of the weather.
 *
 * <p>Like {@link WeatherLayer}, this is part of how the game is shown, not of the game itself: it
 * only follows the weather it is told about. If the computer has no sound, it stays silent.
 */
public final class WeatherSound {

  /** The light rain loop, among the game's resources. */
  static final String LIGHT_RAIN = "/sounds/weather/light-rain.wav";

  /** The heavy rain loop, among the game's resources. */
  static final String HEAVY_RAIN = "/sounds/weather/heavy-rain.wav";

  /** How long a sound takes to fade fully in or out, in seconds. */
  static final double FADE_SECONDS = 2.5;

  private final SoundTrack lightRain;
  private final SoundTrack heavyRain;
  private double lightLevel;
  private double heavyLevel;
  private double lightTarget;
  private double heavyTarget;

  /**
   * Creates the weather's sound from two tracks; used by tests.
   *
   * @param lightRain plays while it rains
   * @param heavyRain plays in a thunderstorm
   */
  WeatherSound(SoundTrack lightRain, SoundTrack heavyRain) {
    this.lightRain = lightRain;
    this.heavyRain = heavyRain;
  }

  /**
   * Loads the weather's sounds from the game's resources. Silent, but still usable, if they or the
   * computer's sound are missing.
   *
   * @return the weather's sound, silent until the weather turns wet
   */
  public static WeatherSound load() {
    return new WeatherSound(ClipTrack.load(LIGHT_RAIN), ClipTrack.load(HEAVY_RAIN));
  }

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
    lightRain.setLevel(lightLevel);
    heavyRain.setLevel(heavyLevel);
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

  /** Stops every sound for good. */
  public void close() {
    lightRain.close();
    heavyRain.close();
  }

  private static double toward(double level, double target, double step) {
    return level + Math.max(-step, Math.min(step, target - level));
  }
}
