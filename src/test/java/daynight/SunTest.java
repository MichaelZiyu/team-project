package daynight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SunTest {

  private static final double PLAYER_HEIGHT = 48;

  @Test
  void sunFollowsItsClock() {
    DayClock clock = new DayClock(1000, 0.5);
    Sun sun = new Sun(clock);
    assertEquals(SunPosition.at(0.5), sun.position());
    clock.setTimeOfDay(0.3);
    assertEquals(SunPosition.at(0.3), sun.position());
    assertSame(clock, sun.clock());
  }

  @Test
  void morningShadowsFallWestAndEveningShadowsEast() {
    Sun sun = new Sun(new DayClock(1000, 0.3));
    assertTrue(sun.shadowX(PLAYER_HEIGHT) < 0, "morning: west");
    assertTrue(sun.shadowY(PLAYER_HEIGHT) < 0, "and a little south");
    sun.clock().setTimeOfDay(0.7);
    assertTrue(sun.shadowX(PLAYER_HEIGHT) > 0, "evening: east");
    assertTrue(sun.shadowY(PLAYER_HEIGHT) < 0, "and a little south");
  }

  @Test
  void noonShadowIsShortAndStraightSouth() {
    Sun sun = new Sun(new DayClock(1000, 0.5));
    assertEquals(0, sun.shadowX(PLAYER_HEIGHT), 1e-9);
    double expected = PLAYER_HEIGHT / Math.tan(SunPosition.MAX_ELEVATION);
    assertEquals(-expected, sun.shadowY(PLAYER_HEIGHT), 1e-9);
    assertEquals(expected, sun.shadowLength(PLAYER_HEIGHT), 1e-9);
    assertEquals(1, sun.daylight());
  }

  @Test
  void shadowLengthGrowsWithHeightAndIsCappedNearTheHorizon() {
    Sun sun = new Sun(new DayClock(1000, 0.4));
    assertEquals(2 * sun.shadowLength(10), sun.shadowLength(20), 1e-9);
    sun.clock().setTimeOfDay(0.252);
    assertEquals(
        SunPosition.MAX_SHADOW_LENGTH_RATIO * PLAYER_HEIGHT,
        sun.shadowLength(PLAYER_HEIGHT),
        1e-9);
    assertTrue(sun.daylight() < 0.2, "a capped shadow is faint");
  }

  @Test
  void nothingCastsAShadowAtNight() {
    Sun sun = new Sun(new DayClock(1000, 0.95));
    assertEquals(0, sun.shadowLength(PLAYER_HEIGHT));
    assertEquals(0, sun.shadowX(PLAYER_HEIGHT), 1e-12);
    assertEquals(0, sun.shadowY(PLAYER_HEIGHT), 1e-12);
    assertEquals(0, sun.daylight());
  }

  @Test
  void sunNeedsAClock() {
    assertThrows(NullPointerException.class, () -> new Sun(null));
  }
}
