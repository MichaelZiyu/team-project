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
}
