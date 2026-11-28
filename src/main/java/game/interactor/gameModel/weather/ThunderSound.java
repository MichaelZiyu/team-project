package game.interactor.gameModel.weather;

import game.interactor.outputData.RenderableData;
import game.interactor.outputData.RenderableData.SoundEvent;
import java.util.ArrayList;
import java.util.Collections;
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
 * <p>Call {@link #strike} as each flash begins and {@link #update} every tick. Nothing is played
 * here: once its delay is over, a thunderclap becomes a {@link SoundEvent} ({@link #events}), kept
 * for {@link RenderableData#SOUND_EVENT_SECONDS} so the renderer cannot miss it, and the game's
 * audio plays it once.
 */
public final class ThunderSound {

  /** The thunder sounds, named by their files under the sound folder; each starts at its clap. */
  static final List<String> SOUNDS = List.of("weather/thunder-close", "weather/thunder-rumble");

  /** How long after the weakest, furthest flash its thunder comes, in seconds. */
  static final double MAX_DELAY_SECONDS = 4;

  /** A thunderclap on its way, or one heard and kept for a moment as a sound event. */
  private static final class Clap {
    private double seconds;
    private final String sound;
    private SoundEvent event;

    Clap(double secondsLeft, String sound) {
      this.seconds = secondsLeft;
      this.sound = sound;
    }
  }

  private final Random random;
  private final List<Clap> claps = new ArrayList<>();

  /**
   * Creates thunder.
   *
   * @param seed seeds which sound each strike plays
   */
  public ThunderSound(long seed) {
    this.random = new Random(seed);
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
    claps.add(new Clap(delayFor(intensity), SOUNDS.get(random.nextInt(SOUNDS.size()))));
  }

  /**
   * Moves time forward: thunder whose delay is over becomes a sound event, and sound events older
   * than {@link RenderableData#SOUND_EVENT_SECONDS} are dropped.
   *
   * @param seconds how much time passed since the last update, in seconds
   */
  public void update(double seconds) {
    Iterator<Clap> all = claps.iterator();
    while (all.hasNext()) {
      Clap clap = all.next();
      clap.seconds -= seconds;
      if (clap.event == null && clap.seconds <= 0) {
        clap.event = new SoundEvent(RenderableData.nextSoundEventNumber(), clap.sound, 1);
        // From now on, it counts how long the event has been kept.
        clap.seconds = 0;
      } else if (clap.event != null && -clap.seconds >= RenderableData.SOUND_EVENT_SECONDS) {
        all.remove();
      }
    }
  }

  /**
   * Returns the thunderclaps heard in the last {@link RenderableData#SOUND_EVENT_SECONDS}, for the
   * frame the game hands to the renderer.
   *
   * @return the sound events, oldest first
   */
  public List<SoundEvent> events() {
    List<SoundEvent> events = new ArrayList<>();
    for (Clap clap : claps) {
      if (clap.event != null) {
        events.add(clap.event);
      }
    }
    return Collections.unmodifiableList(events);
  }

  /**
   * Returns how many thunderclaps are on their way.
   *
   * @return the number scheduled but not yet heard
   */
  public int pendingCount() {
    int pending = 0;
    for (Clap clap : claps) {
      if (clap.event == null) {
        pending++;
      }
    }
    return pending;
  }
}
