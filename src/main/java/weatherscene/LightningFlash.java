package weatherscene;

import java.util.Random;

/**
 * Lightning during a thunderstorm, striking every few seconds at random. Each strike jumps straight
 * to full brightness and holds it for a few frames, then fades fast; a second, weaker flicker
 * follows the same way.
 */
public final class LightningFlash {

  /** The shortest wait between strikes, in seconds. */
  static final double MIN_GAP_SECONDS = 4;

  /** The longest wait between strikes, in seconds. */
  static final double MAX_GAP_SECONDS = 12;

  /** How long the main flash stays at full brightness, in seconds: about four frames. */
  static final double FLASH_HOLD_SECONDS = 0.07;

  /** How long the main flash takes to fade after holding, in seconds. */
  static final double FLASH_FADE_SECONDS = 0.15;

  /** When the second, weaker flicker starts after the strike, in seconds. */
  static final double FLICKER_START_SECONDS = 0.3;

  /** How long the second flicker holds, in seconds. */
  static final double FLICKER_HOLD_SECONDS = 0.04;

  /** How long the second flicker takes to fade, in seconds. */
  static final double FLICKER_FADE_SECONDS = 0.12;

  /** How bright the second flicker is, compared with the main flash. */
  static final double FLICKER_BRIGHTNESS = 0.7;

  private final Random random;
  private boolean active;
  private double untilNextStrike;
  private double sinceStrike = Double.POSITIVE_INFINITY;

  /**
   * Creates lightning that is switched off.
   *
   * @param seed seeds the timing of strikes, so tests can repeat a storm exactly
   */
  public LightningFlash(long seed) {
    this.random = new Random(seed);
  }

  /**
   * Switches strikes on or off. A flash already showing still fades out.
   *
   * @param active {@code true} during a thunderstorm
   */
  public void setActive(boolean active) {
    if (active && !this.active) {
      untilNextStrike = nextGap();
    }
    this.active = active;
  }

  /**
   * Returns whether lightning strikes are switched on.
   *
   * @return {@code true} during a thunderstorm
   */
  public boolean isActive() {
    return active;
  }

  /** Strikes right now, whatever the timing. */
  public void strikeNow() {
    sinceStrike = 0;
  }

  /**
   * Moves time forward, striking when the wait is over.
   *
   * @param seconds how much time passed, in seconds
   */
  public void update(double seconds) {
    sinceStrike += seconds;
    if (!active) {
      return;
    }
    untilNextStrike -= seconds;
    if (untilNextStrike <= 0) {
      strikeNow();
      untilNextStrike = nextGap();
    }
  }

  /**
   * Returns how bright the flash is right now.
   *
   * @return 1 at the moment of a strike, fading to 0
   */
  public double brightness() {
    double main = pulse(sinceStrike, FLASH_HOLD_SECONDS, FLASH_FADE_SECONDS);
    double sinceFlicker = sinceStrike - FLICKER_START_SECONDS;
    double flicker =
        FLICKER_BRIGHTNESS * pulse(sinceFlicker, FLICKER_HOLD_SECONDS, FLICKER_FADE_SECONDS);
    return Math.max(main, flicker);
  }

  /**
   * Returns a flash's brightness {@code time} seconds after it starts: full at once, held for
   * {@code hold} seconds, then fading to nothing over {@code fade} seconds; 0 before it starts.
   */
  private static double pulse(double time, double hold, double fade) {
    if (time < 0 || time >= hold + fade) {
      return 0;
    }
    if (time < hold) {
      return 1;
    }
    return 1 - (time - hold) / fade;
  }

  private double nextGap() {
    return MIN_GAP_SECONDS + random.nextDouble() * (MAX_GAP_SECONDS - MIN_GAP_SECONDS);
  }
}
