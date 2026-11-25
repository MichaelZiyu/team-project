package daynight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DayClockTest {

  @Test
  void newClockStartsAtSevenInTheMorningOnDayOne() {
    DayClock clock = new DayClock(2400);
    assertEquals(7, clock.hour());
    assertEquals(0, clock.minute());
    assertEquals(1, clock.day());
    assertEquals(DayClock.START_TIME, clock.timeOfDay(), 1e-9);
  }

  @Test
  void ticksAdvanceTimeAndRollOverAtMidnight() {
    DayClock clock = new DayClock(100, 0.98);
    clock.tick();
    assertEquals(0.99, clock.timeOfDay(), 1e-9);
    clock.tick();
    assertEquals(0, clock.timeOfDay(), 1e-9);
    assertEquals(2, clock.day());
  }

  @Test
  void tenMinuteDayAtSixtyTicksPerSecondHas36000Ticks() {
    DayClock clock = DayClock.forRealTime(600, 1.0 / 60);
    assertEquals(36_000, clock.ticksPerDay());
    clock.setTimeOfDay(0);
    for (int i = 0; i < 18_000; i++) {
      clock.tick();
    }
    assertEquals(12, clock.hour(), "half a day after midnight is noon");
  }

  @Test
  void settingTheTimeKeepsTheDay() {
    DayClock clock = new DayClock(1000, 0.9);
    for (int i = 0; i < 200; i++) {
      clock.tick();
    }
    assertEquals(2, clock.day());
    clock.setTimeOfDay(0.5);
    assertEquals(2, clock.day());
    assertEquals(12, clock.hour());
    assertEquals(0, clock.minute());
  }

  @Test
  void invalidValuesAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> new DayClock(0));
    assertThrows(IllegalArgumentException.class, () -> new DayClock(10, 1.0));
    assertThrows(IllegalArgumentException.class, () -> new DayClock(10, -0.1));
    assertThrows(IllegalArgumentException.class, () -> new DayClock(10).setTimeOfDay(Double.NaN));
  }
}
