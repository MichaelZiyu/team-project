package weatherscene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Lightning during a thunderstorm, timed like the real thing.
 *
 * <p>Each flash is made of 1 to 5 <em>strokes</em> down the same channel, a few dozen milliseconds
 * apart. Each stroke jumps straight to its peak, holds it for three frames so the glare registers
 * and can never fall between frames, then fades away exponentially within a few dozen
 * milliseconds. The first stroke
 * is the brightest; later ones are weaker. About one flash in three also has a <em>continuing
 * current</em>: after one of its strokes, a dimmer glow lingers for a fraction of a second before
 * fading. Flashes come at random, an exponential distribution of waits as for any independent
 * events, but never closer together than {@link #MIN_GAP_SECONDS}.
 *
 * <p>Each flash also comes from a random direction, with a random overall strength, both chosen
 * as soon as the previous flash starts, so anything that needs time to prepare for it (such as
 * painting the terrain lit by the strike) can start early. A flash that is due can be held back
 * with {@link #setReady} until that is done.
 */
public final class LightningFlash {

  /** How likely a flash is to have 1, 2, 3, 4 or 5 strokes. */
  static final double[] STROKE_COUNT_CHANCES = {0.15, 0.25, 0.25, 0.20, 0.15};

  /** The usual gap between strokes, in seconds; gaps spread around it, a few much longer. */
  static final double MEDIAN_STROKE_GAP_SECONDS = 0.09;

  /** How widely gaps between strokes spread: the standard deviation of their logarithm. */
  static final double STROKE_GAP_SPREAD = 0.45;

  /** The shortest gap between strokes, in seconds. */
  static final double MIN_STROKE_GAP_SECONDS = 0.0375;

  /** The longest gap between strokes, in seconds. */
  static final double MAX_STROKE_GAP_SECONDS = 0.375;

  /** The weakest a later stroke can be, compared with the first. */
  static final double MIN_LATER_STROKE = 0.3;

  /** The strongest a later stroke can be, compared with the first. */
  static final double MAX_LATER_STROKE = 0.9;

  /** The quickest a stroke can fade: the time to drop to about a third, in seconds. */
  static final double MIN_DECAY_SECONDS = 0.0375;

  /** The slowest a stroke can fade: the time to drop to about a third, in seconds. */
  static final double MAX_DECAY_SECONDS = 0.09;

  /**
   * How long each stroke holds its peak, in seconds: three frames, long enough for the glare to
   * register, and never short enough to fall between frames.
   */
  static final double PEAK_SECONDS = 3.0 / 60;

  /** How likely a flash is to have a continuing current. */
  static final double CONTINUING_CURRENT_CHANCE = 1.0 / 3;

  /** The dimmest a continuing current's glow can be, compared with its stroke. */
  static final double MIN_GLOW = 0.3;

  /** The brightest a continuing current's glow can be, compared with its stroke. */
  static final double MAX_GLOW = 0.5;

  /** The shortest a continuing current lasts before fading, in seconds. */
  static final double MIN_GLOW_SECONDS = 0.09;

  /** The longest a continuing current lasts before fading, in seconds. */
  static final double MAX_GLOW_SECONDS = 0.375;

  /** The average wait from one flash to the next, in seconds. */
  static final double MEAN_GAP_SECONDS = 8;

  /** The shortest wait from one flash to the next, in seconds. */
  static final double MIN_GAP_SECONDS = 1.5;

  /** The weakest a flash can be overall, compared with the strongest. */
  static final double MIN_INTENSITY = 0.75;

  /**
   * How bright a moment must be, compared with its flash's peak, to count as bright: the moments
   * to show anything lit by the strike itself.
   */
  static final double BRIGHT_SHARE = 0.5;

  /**
   * One stroke of a flash.
   *
   * @param start when it strikes, in seconds after the flash begins
   * @param amplitude its peak, compared with the flash's first stroke
   * @param decay how fast it fades: the time to drop to about a third, in seconds
   * @param glow the level of the continuing current after it, compared with the first stroke; 0
   *     if there is none
   * @param glowSeconds how long the continuing current lasts before fading, in seconds
   */
  record Stroke(double start, double amplitude, double decay, double glow, double glowSeconds) {

    /**
     * Returns this stroke's brightness at a time after the flash begins: 0 before it strikes and
     * once it has faded below a thousandth.
     */
    double brightnessAt(double time) {
      double since = time - start;
      if (since < 0 || time >= end()) {
        return 0;
      }
      if (since < PEAK_SECONDS) {
        return amplitude;
      }
      double fading = since - PEAK_SECONDS;
      double flash = amplitude * Math.exp(-fading / decay);
      if (glow <= 0) {
        return flash;
      }
      double afterGlow = Math.max(0, fading - glowSeconds);
      return Math.max(flash, glow * Math.exp(-afterGlow / decay));
    }

    /** Returns when this stroke has faded to nothing visible, in seconds after the flash begins. */
    double end() {
      // Seven decay times brings it below a thousandth of its peak.
      return start + PEAK_SECONDS + glowSeconds + 7 * decay;
    }
  }

  /** A planned flash: where it comes from, how strong it is overall, and its strokes. */
  private record Flash(double angle, double intensity, List<Stroke> strokes) {

    double brightnessAt(double time) {
      double brightest = 0;
      for (Stroke stroke : strokes) {
        brightest = Math.max(brightest, stroke.brightnessAt(time));
      }
      return intensity * brightest;
    }
  }

  private final Random random;
  private boolean active;
  private boolean ready = true;
  private int strikes;
  private double untilNextStrike;
  private double sinceStrike = Double.POSITIVE_INFINITY;
  private Flash upcoming;
  private Flash current;

  /**
   * Creates lightning that is switched off.
   *
   * @param seed seeds the timing, shape and direction of flashes, so tests can repeat a storm
   */
  public LightningFlash(long seed) {
    this.random = new Random(seed);
    this.upcoming = planFlash();
  }

  /**
   * Switches flashes on or off. A flash already showing still fades out.
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
   * Returns whether flashes are switched on.
   *
   * @return {@code true} during a thunderstorm
   */
  public boolean isActive() {
    return active;
  }

  /** Starts the planned flash right now, whatever the timing and whether or not it is ready. */
  public void strikeNow() {
    strikes++;
    sinceStrike = 0;
    current = upcoming;
    upcoming = planFlash();
  }

  /** Makes the next flash due now; it still waits until {@link #setReady ready}. */
  public void strikeSoon() {
    untilNextStrike = 0;
  }

  /**
   * Says whether a due flash may go ahead. While not ready, a flash that is due waits.
   *
   * @param ready {@code true} once everything the flash needs is prepared
   */
  public void setReady(boolean ready) {
    this.ready = ready;
  }

  /**
   * Returns the direction of the next flash, chosen in advance.
   *
   * @return the direction its shadows will fall, in radians counterclockwise from east
   */
  public double upcomingAngle() {
    return upcoming.angle();
  }

  /**
   * Returns how strong the next flash will be overall, chosen in advance.
   *
   * @return from {@link #MIN_INTENSITY} to 1 (full)
   */
  public double upcomingIntensity() {
    return upcoming.intensity();
  }

  /**
   * Returns the direction of the latest flash.
   *
   * @return the direction its shadows fall, in radians counterclockwise from east; 0 before any
   */
  public double strikeAngle() {
    return current == null ? 0 : current.angle();
  }

  /**
   * Returns how strong the latest flash is overall: the brightness of its first stroke.
   *
   * @return from {@link #MIN_INTENSITY} to 1 (full); 1 before any
   */
  public double strikeIntensity() {
    return current == null ? 1 : current.intensity();
  }

  /**
   * Returns how long until the next flash is due.
   *
   * @return seconds; 0 if it is due now (perhaps waiting until ready), infinite if lightning is off
   */
  public double secondsUntilStrike() {
    return active ? Math.max(0, untilNextStrike) : Double.POSITIVE_INFINITY;
  }

  /**
   * Moves time forward, starting the next flash when the wait is over and it is ready.
   *
   * @param seconds how much time passed, in seconds
   */
  public void update(double seconds) {
    sinceStrike += seconds;
    if (!active) {
      return;
    }
    untilNextStrike -= seconds;
    if (untilNextStrike <= 0 && ready) {
      strikeNow();
      untilNextStrike = nextGap();
    }
  }

  /**
   * Returns how bright the lightning is right now.
   *
   * @return from 0 (dark) to the flash's overall strength, at the peak of its first stroke
   */
  public double brightness() {
    return current == null ? 0 : current.brightnessAt(sinceStrike);
  }

  /**
   * Returns whether this is one of a flash's bright moments: a stroke at least {@link
   * #BRIGHT_SHARE} as bright as the flash's peak. These are the moments to show anything lit by the
   * strike itself.
   *
   * @return {@code true} during the brightest part of a strong stroke
   */
  public boolean isBright() {
    return current != null && brightness() >= BRIGHT_SHARE * current.intensity();
  }

  /**
   * Returns how many flashes have begun so far.
   *
   * @return the number of strikes since this lightning was made
   */
  public int strikeCount() {
    return strikes;
  }

  /**
   * Returns the strokes of the next flash, planned in advance.
   *
   * @return its strokes, in order
   */
  List<Stroke> upcomingStrokes() {
    return upcoming.strokes();
  }

  /** Plans a flash: its direction, overall strength, strokes and any continuing current. */
  private Flash planFlash() {
    double angle = random.nextDouble() * 2 * Math.PI;
    double intensity = between(MIN_INTENSITY, 1);
    int count = strokeCount();
    int glowAfter =
        random.nextDouble() < CONTINUING_CURRENT_CHANCE ? random.nextInt(count) : -1;
    List<Stroke> strokes = new ArrayList<>();
    double start = 0;
    for (int i = 0; i < count; i++) {
      if (i > 0) {
        start += strokeGap();
      }
      double amplitude = i == 0 ? 1 : between(MIN_LATER_STROKE, MAX_LATER_STROKE);
      double decay = between(MIN_DECAY_SECONDS, MAX_DECAY_SECONDS);
      double glow = 0;
      double glowSeconds = 0;
      if (i == glowAfter) {
        glow = amplitude * between(MIN_GLOW, MAX_GLOW);
        glowSeconds = between(MIN_GLOW_SECONDS, MAX_GLOW_SECONDS);
      }
      strokes.add(new Stroke(start, amplitude, decay, glow, glowSeconds));
    }
    return new Flash(angle, intensity, Collections.unmodifiableList(strokes));
  }

  /** Picks how many strokes a flash has, with the chances in {@link #STROKE_COUNT_CHANCES}. */
  private int strokeCount() {
    double pick = random.nextDouble();
    for (int i = 0; i < STROKE_COUNT_CHANCES.length; i++) {
      pick -= STROKE_COUNT_CHANCES[i];
      if (pick < 0) {
        return i + 1;
      }
    }
    return STROKE_COUNT_CHANCES.length;
  }

  /** Picks a gap between strokes: lognormal around the median, kept within the limits. */
  private double strokeGap() {
    double gap =
        MEDIAN_STROKE_GAP_SECONDS * Math.exp(STROKE_GAP_SPREAD * random.nextGaussian());
    return Math.max(MIN_STROKE_GAP_SECONDS, Math.min(MAX_STROKE_GAP_SECONDS, gap));
  }

  /** Picks the wait until the next flash: exponential around the mean, but not too short. */
  private double nextGap() {
    double wait = -MEAN_GAP_SECONDS * Math.log(1 - random.nextDouble());
    return Math.max(MIN_GAP_SECONDS, wait);
  }

  private double between(double low, double high) {
    return low + random.nextDouble() * (high - low);
  }
}
