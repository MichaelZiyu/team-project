package weatherscene;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Thunder after each lightning strike.
 *
 * <p>Light arrives at once, but sound takes about three seconds to travel a kilometre, so thunder
 * follows its flash after a delay that grows with the strike's distance. A flash's overall
 * strength stands in for how close it was: the strongest flash ({@code 1}) is overhead and
 * thunders straight away; the weakest ({@link LightningFlash#MIN_INTENSITY}) is furthest, and
 * thunders {@link #MAX_DELAY_SECONDS} later; those between are in proportion. Each strike plays
 * one of the thunder sounds, picked at random.
 *
 * <p>Call {@link #strike} as each flash begins and {@link #update} every frame. Like {@link
 * WeatherSound}, it is part of how the game is shown, and stays silent without a sound device.
 */
public final class ThunderSound {

  /** The thunder sounds, among the game's resources: each starts at its clap. */
  static final String[] SOUNDS = {
    "/sounds/weather/thunder-close.wav", "/sounds/weather/thunder-rumble.wav"
  };

  /** How long after the weakest, furthest flash its thunder comes, in seconds. */
  static final double MAX_DELAY_SECONDS = 4;

  /** A thunderclap waiting for its sound to arrive. */
  private static final class Pending {
    private double secondsLeft;
    private final int sound;

    Pending(double secondsLeft, int sound) {
      this.secondsLeft = secondsLeft;
      this.sound = sound;
    }
  }

  private final ThunderPlayer player;
  private final Random random;
  private final List<Pending> pending = new ArrayList<>();

  /**
   * Creates thunder with a given player; used by tests.
   *
   * @param player plays the sounds
   * @param seed seeds which sound each strike plays
   */
  ThunderSound(ThunderPlayer player, long seed) {
    this.player = player;
    this.random = new Random(seed);
  }

  /**
   * Loads the thunder sounds from the game's resources.
   *
   * @param seed seeds which sound each strike plays
   * @return the thunder, silent if the sounds or the computer's sound are missing
   */
  public static ThunderSound load(long seed) {
    return new ThunderSound(new ClipThunderPlayer(SOUNDS), seed);
  }

  /**
   * Returns how long after a flash its thunder comes.
   *
   * @param intensity the flash's overall strength, from {@link LightningFlash#MIN_INTENSITY} to 1
   * @return from 0 (the strongest flash) to {@link #MAX_DELAY_SECONDS} (the weakest), in seconds
   */
  public static double delayFor(double intensity) {
    double min = LightningFlash.MIN_INTENSITY;
    double closeness = Math.max(0, Math.min(1, (intensity - min) / (1 - min)));
    return MAX_DELAY_SECONDS * (1 - closeness);
  }

  /**
   * Schedules thunder for a flash that has just begun.
   *
   * @param intensity the flash's overall strength, which sets how long until its thunder
   */
  public void strike(double intensity) {
    if (player.soundCount() == 0) {
      return;
    }
    pending.add(new Pending(delayFor(intensity), random.nextInt(player.soundCount())));
  }

  /**
   * Moves time forward, playing any thunder whose delay is over.
   *
   * @param seconds how much time passed since the last update, in seconds
   */
  public void update(double seconds) {
    Iterator<Pending> waiting = pending.iterator();
    while (waiting.hasNext()) {
      Pending thunder = waiting.next();
      thunder.secondsLeft -= seconds;
      if (thunder.secondsLeft <= 0) {
        waiting.remove();
        player.play(thunder.sound);
      }
    }
  }

  /**
   * Returns how many thunderclaps are on their way.
   *
   * @return the number scheduled but not yet heard
   */
  public int pendingCount() {
    return pending.size();
  }

  /** Stops all thunder for good. */
  public void close() {
    pending.clear();
    player.close();
  }
}
