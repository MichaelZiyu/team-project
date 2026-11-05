package terrainscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import daynight.SunPosition;
import org.junit.jupiter.api.Test;

class ShadowCastTest {

  @Test
  void morningShadowsPointWestAndEveningShadowsPointEast() {
    ShadowCast morning = ShadowCast.from(SunPosition.at(0.3));
    assertTrue(morning.dxPerLevel() < 0, "morning shadows go left");
    assertTrue(morning.dyPerLevel() < 0, "and a little south");
    ShadowCast evening = ShadowCast.from(SunPosition.at(0.7));
    assertTrue(evening.dxPerLevel() > 0, "evening shadows go right");
    assertTrue(evening.dyPerLevel() < 0, "and a little south");
  }

  @Test
  void noonShadowsAreShortAndPointSouth() {
    ShadowCast noon = ShadowCast.from(SunPosition.at(0.5));
    assertEquals(0, noon.dxPerLevel(), 1e-9);
    assertTrue(noon.dyPerLevel() < 0);
    double length = Math.hypot(noon.dxPerLevel(), noon.dyPerLevel());
    assertTrue(length < TileRenderable.STEP_HEIGHT_PX / 2.0, "short at noon: " + length);
  }

  @Test
  void lowSunShadowsAreCappedAndFaint() {
    ShadowCast dawn = ShadowCast.from(SunPosition.at(0.2501));
    double length = Math.hypot(dawn.dxPerLevel(), dawn.dyPerLevel());
    assertEquals(ShadowCast.MAX_LENGTH_PER_LEVEL_PX, length, 1e-9);
    assertTrue(dawn.strength() < 0.05);
  }

  @Test
  void terrainShadowsMatchTheSunsShadowForOneStep() {
    for (double time = 0.26; time < 0.75; time += 0.05) {
      SunPosition sun = SunPosition.at(time);
      ShadowCast cast = ShadowCast.from(sun);
      assertEquals(sun.shadowX(TileRenderable.STEP_HEIGHT_PX), cast.dxPerLevel(), 1e-9);
      assertEquals(sun.shadowY(TileRenderable.STEP_HEIGHT_PX), cast.dyPerLevel(), 1e-9);
      assertEquals(sun.daylight(), cast.strength(), 1e-9);
    }
  }

  @Test
  void nightHasNoSunlight() {
    assertEquals(ShadowCast.NIGHT, ShadowCast.from(SunPosition.at(0)));
    assertFalse(ShadowCast.NIGHT.isDaylight());
    for (Edge edge : Edge.values()) {
      assertEquals(0, ShadowCast.NIGHT.edgeLight(edge));
    }
  }

  @Test
  void edgesFacingTheSunAreLitAndOthersAreNot() {
    // The default sun is to the right and a little north.
    assertEquals(1, ShadowCast.DEFAULT.edgeLight(Edge.RIGHT), 1e-9);
    double back = ShadowCast.DEFAULT.edgeLight(Edge.BACK);
    assertTrue(back > 0.5 && back < 1, "the back edge half faces the sun: " + back);
    assertEquals(0, ShadowCast.DEFAULT.edgeLight(Edge.LEFT));
    assertEquals(0, ShadowCast.DEFAULT.edgeLight(Edge.FRONT));
    ShadowCast evening = ShadowCast.from(SunPosition.at(0.7));
    assertTrue(evening.edgeLight(Edge.LEFT) > 0.9);
    assertEquals(0, evening.edgeLight(Edge.RIGHT));
  }

  @Test
  void edgeLightChangesGraduallyThroughTheDay() {
    for (Edge edge : Edge.values()) {
      double previous = ShadowCast.from(SunPosition.at(0)).edgeLight(edge);
      for (double time = 0.0005; time < 1; time += 0.0005) {
        double now = ShadowCast.from(SunPosition.at(time)).edgeLight(edge);
        assertTrue(Math.abs(now - previous) < 0.03, edge + " jumps at " + time);
        previous = now;
      }
    }
  }

  @Test
  void dimSunlightDimsLitEdges() {
    ShadowCast half = new ShadowCast(-32, 0, 0.5);
    assertEquals(0.5, half.edgeLight(Edge.RIGHT), 1e-9);
  }
}
