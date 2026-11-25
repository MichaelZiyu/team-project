package daynight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SunPositionTest {

  @Test
  void sunRisesInTheEastPassesNorthAtNoonAndSetsInTheWest() {
    SunPosition sunrise = SunPosition.at(0.25);
    assertEquals(1, sunrise.towardSunX(), 1e-9);
    assertEquals(0, sunrise.elevation(), 1e-9);
    SunPosition noon = SunPosition.at(0.5);
    assertEquals(1, noon.towardSunY(), 1e-9);
    assertEquals(SunPosition.MAX_ELEVATION, noon.elevation(), 1e-9);
    SunPosition sunset = SunPosition.at(0.75);
    assertEquals(-1, sunset.towardSunX(), 1e-9);
    assertEquals(0, sunset.elevation(), 1e-9);
  }

  @Test
  void sunIsBelowTheHorizonAtNight() {
    assertTrue(SunPosition.at(0).elevation() < 0);
    assertTrue(SunPosition.at(0.1).elevation() < 0);
    assertTrue(SunPosition.at(0.9).elevation() < 0);
    assertEquals(0, SunPosition.at(0).daylight());
  }

  @Test
  void daylightFadesInAtDawnAndIsFullDuringTheDay() {
    assertEquals(0, SunPosition.at(0.25).daylight(), 1e-9);
    double earlyDawn = SunPosition.at(0.26).daylight();
    assertTrue(earlyDawn > 0 && earlyDawn < 1, "fading in: " + earlyDawn);
    assertEquals(1, SunPosition.at(0.35).daylight());
    assertEquals(1, SunPosition.at(0.5).daylight());
    assertEquals(1, SunPosition.at(0.65).daylight());
  }

  @Test
  void azimuthStaysWithinOneTurn() {
    for (double time = 0; time < 1; time += 0.01) {
      double azimuth = SunPosition.at(time).azimuth();
      assertTrue(azimuth >= 0 && azimuth < 2 * Math.PI, "at " + time + ": " + azimuth);
    }
  }

  @Test
  void goldenHourIsWhenTheSunIsLow() {
    assertEquals(0, SunPosition.at(0.5).goldenHour(), "midday");
    assertEquals(0, SunPosition.at(0.0).goldenHour(), "midnight");
    assertEquals(0, SunPosition.at(0.76).goldenHour(), "just after sunset");
    // A sun 7 degrees up, before sunset and after sunrise.
    assertEquals(1, new SunPosition(Math.PI, Math.toRadians(7)).goldenHour(), 1e-9);
    assertEquals(1, new SunPosition(0, Math.toRadians(7)).goldenHour(), 1e-9);
  }

  @Test
  void goldenHourComesAndGoesSmoothly() {
    double previous = 0;
    // Through the afternoon toward sunset: rising, then fading in only at the very end.
    boolean rose = false;
    for (double time = 0.6; time < 0.75; time += 0.001) {
      double golden = SunPosition.at(time).goldenHour();
      assertTrue(golden >= 0 && golden <= 1);
      assertTrue(Math.abs(golden - previous) < 0.2, "no sudden jump at " + time);
      rose |= golden > previous;
      previous = golden;
    }
    assertTrue(rose);
    assertEquals(0, SunPosition.at(0.6).goldenHour(), "mid-afternoon");
  }
}
