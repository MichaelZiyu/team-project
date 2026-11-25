package daynight;

import java.io.Serializable;

/**
 * The time of day in the game world, advanced one step per game tick.
 *
 * <p>Time of day is a fraction of a day: {@code 0} is midnight, {@code 0.25} is 6:00, {@code 0.5}
 * is noon and {@code 0.75} is 18:00. The clock only moves when {@link #tick()} is called, so it
 * stands still while the game is paused, and it is saved with the world.
 */
public final class DayClock implements Serializable {

  /** The time a new world starts at: 7:00 in the morning. */
  public static final double START_TIME = 7.0 / 24;

  private static final long serialVersionUID = 1L;

  private static final int HOURS_PER_DAY = 24;
  private static final int MINUTES_PER_HOUR = 60;

  private final long ticksPerDay;

  /** Ticks since midnight at the start of day 1. */
  private long ticks;

  /**
   * Creates a clock at {@link #START_TIME} on day 1.
   *
   * @param ticksPerDay how many ticks one full day and night lasts; at least 1
   * @throws IllegalArgumentException if {@code ticksPerDay} is less than 1
   */
  public DayClock(long ticksPerDay) {
    this(ticksPerDay, START_TIME);
  }

  /**
   * Creates a clock at a given time on day 1.
   *
   * @param ticksPerDay how many ticks one full day and night lasts; at least 1
   * @param timeOfDay the starting time, from 0 (inclusive) to 1 (exclusive)
   * @throws IllegalArgumentException if {@code ticksPerDay} is less than 1 or the time is out of
   *     range
   */
  public DayClock(long ticksPerDay, double timeOfDay) {
    if (ticksPerDay < 1) {
      throw new IllegalArgumentException("ticksPerDay must be at least 1: " + ticksPerDay);
    }
    this.ticksPerDay = ticksPerDay;
    setTimeOfDay(timeOfDay);
  }

  /**
   * Creates a clock whose day lasts a given number of real seconds.
   *
   * @param dayLengthSeconds how long one full day and night lasts, in real seconds
   * @param secondsPerTick how long one game tick lasts, in real seconds
   * @return a clock at {@link #START_TIME} on day 1
   * @throws IllegalArgumentException if the day is shorter than one tick
   */
  public static DayClock forRealTime(double dayLengthSeconds, double secondsPerTick) {
    return new DayClock(Math.round(dayLengthSeconds / secondsPerTick));
  }

  /** Moves the clock forward by one tick, rolling over into the next day at midnight. */
  public void tick() {
    ticks++;
  }

  /**
   * Returns the time of day.
   *
   * @return a fraction from 0 (inclusive, midnight) to 1 (exclusive)
   */
  public double timeOfDay() {
    return (double) (ticks % ticksPerDay) / ticksPerDay;
  }

  /**
   * Moves the clock to a time on the current day, for example after sleeping or for testing.
   *
   * @param timeOfDay the new time, from 0 (inclusive) to 1 (exclusive)
   * @throws IllegalArgumentException if the time is out of range
   */
  public void setTimeOfDay(double timeOfDay) {
    if (!(timeOfDay >= 0 && timeOfDay < 1)) {
      throw new IllegalArgumentException("timeOfDay must be in [0, 1): " + timeOfDay);
    }
    long startOfDay = ticks - ticks % ticksPerDay;
    ticks = startOfDay + Math.min(ticksPerDay - 1, Math.round(timeOfDay * ticksPerDay));
  }

  /**
   * Returns which day it is.
   *
   * @return the day number, starting at 1
   */
  public long day() {
    return ticks / ticksPerDay + 1;
  }

  /**
   * Returns the hour on a 24-hour clock.
   *
   * @return the hour, from 0 to 23
   */
  public int hour() {
    return (int) (timeOfDay() * HOURS_PER_DAY);
  }

  /**
   * Returns the minute within the hour.
   *
   * @return the minute, from 0 to 59
   */
  public int minute() {
    return (int) (timeOfDay() * HOURS_PER_DAY * MINUTES_PER_HOUR) % MINUTES_PER_HOUR;
  }

  /**
   * Returns how many ticks one day lasts.
   *
   * @return the length of a day, in ticks
   */
  public long ticksPerDay() {
    return ticksPerDay;
  }
}
