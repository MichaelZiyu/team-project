package terrainscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TerrainLightTest {

  @Test
  void defaultIsTheMidMorningSun() {
    TerrainLight light = TerrainLight.DEFAULT;
    assertEquals(-32, light.dxPerLevel());
    assertEquals(-16, light.dyPerLevel());
    assertEquals(1, light.sunlight());
    assertEquals(1, light.shadowStrength());
    assertTrue(light.softShadows());
    assertEquals(0, light.topGlow());
    assertEquals(0, light.faceDarkening());
    assertTrue(light.castsShadows());
  }

  @Test
  void cloudDimsSunlightAndShadowsButKeepsTheirDirection() {
    TerrainLight rain = TerrainLight.DEFAULT.overcast(0.85);
    assertEquals(0.15, rain.sunlight(), 1e-9);
    assertEquals(0.15, rain.shadowStrength(), 1e-9);
    assertEquals(TerrainLight.DEFAULT.dxPerLevel(), rain.dxPerLevel());
    assertEquals(TerrainLight.DEFAULT.dyPerLevel(), rain.dyPerLevel());
    assertFalse(TerrainLight.DEFAULT.overcast(1).castsShadows(), "full cloud: no shadows");
    assertThrows(IllegalArgumentException.class, () -> TerrainLight.DEFAULT.overcast(1.5));
  }

  @Test
  void aStrikeHasSharpDarkShadowsGlowingTopsAndDarkFaces() {
    TerrainLight strike = TerrainLight.lightningStrike(Math.PI / 2);
    assertEquals(0, strike.dxPerLevel(), 1e-9);
    assertEquals(TerrainLight.STRIKE_SHADOW_PER_LEVEL_PX, strike.dyPerLevel(), 1e-9);
    assertFalse(strike.softShadows());
    assertTrue(strike.shadowStrength() > 1, "darker than a sunny day's shadows");
    assertTrue(strike.topGlow() > 0);
    assertTrue(strike.faceDarkening() > 0);
    assertEquals(0, strike.sunlight(), "no warm sunlight or sunlit rims");
    for (Edge edge : Edge.values()) {
      assertEquals(0, strike.edgeLight(edge));
    }
  }

  @Test
  void weakerStrikesAreGentlerButCastShadowsJustAsLong() {
    TerrainLight full = TerrainLight.lightningStrike(1);
    TerrainLight quarter = TerrainLight.lightningStrike(1, 0.25);
    assertEquals(full.topGlow() / 4, quarter.topGlow(), 1e-9);
    assertEquals(full.faceDarkening() / 4, quarter.faceDarkening(), 1e-9);
    assertEquals(full.shadowStrength() / 4, quarter.shadowStrength(), 1e-9);
    assertEquals(full.dxPerLevel(), quarter.dxPerLevel(), 1e-9);
    assertEquals(full.dyPerLevel(), quarter.dyPerLevel(), 1e-9);
    assertThrows(IllegalArgumentException.class, () -> TerrainLight.lightningStrike(1, 1.5));
  }

  @Test
  void strikesCanComeFromAnyDirection() {
    TerrainLight east = TerrainLight.lightningStrike(0);
    TerrainLight west = TerrainLight.lightningStrike(Math.PI);
    assertTrue(east.dxPerLevel() > 0 && west.dxPerLevel() < 0);
    double length = Math.hypot(east.dxPerLevel(), east.dyPerLevel());
    assertEquals(TerrainLight.STRIKE_SHADOW_PER_LEVEL_PX, length, 1e-9);
  }

  @Test
  void nightHasNoLight() {
    assertFalse(TerrainLight.NIGHT.castsShadows());
    for (Edge edge : Edge.values()) {
      assertEquals(0, TerrainLight.NIGHT.edgeLight(edge));
    }
  }

  @Test
  void edgesFacingTheLightAreLitAndOthersAreNot() {
    // The default sun is to the right and a little north.
    assertEquals(1, TerrainLight.DEFAULT.edgeLight(Edge.RIGHT), 1e-9);
    double back = TerrainLight.DEFAULT.edgeLight(Edge.BACK);
    assertTrue(back > 0.5 && back < 1, "the back edge half faces the sun: " + back);
    assertEquals(0, TerrainLight.DEFAULT.edgeLight(Edge.LEFT));
    assertEquals(0, TerrainLight.DEFAULT.edgeLight(Edge.FRONT));
    TerrainLight evening = new TerrainLight(40, -20, 1, 1, true, 0, 0);
    assertTrue(evening.edgeLight(Edge.LEFT) > 0.9);
    assertEquals(0, evening.edgeLight(Edge.RIGHT));
  }

  @Test
  void dimSunlightDimsLitEdges() {
    TerrainLight half = new TerrainLight(-32, 0, 0.5, 0.5, true, 0, 0);
    assertEquals(0.5, half.edgeLight(Edge.RIGHT), 1e-9);
  }

  @Test
  void impossibleValuesAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> new TerrainLight(0, 0, 2, 1, true, 0, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new TerrainLight(0, 0, 1, TerrainLight.MAX_SHADOW_STRENGTH + 1, true, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> new TerrainLight(0, 0, 1, 1, true, -1, 0));
    assertThrows(IllegalArgumentException.class, () -> new TerrainLight(0, 0, 1, 1, true, 0, 2));
  }

  /** An evening sun, low in the west: shadows fall east, so the light comes from the west. */
  private static final TerrainLight LOW_EVENING_SUN =
      new TerrainLight(90, 0, 0.15, 0.15, true, 0, 0);

  @Test
  void goldenHourGildsEdgesFacingTheSun() {
    TerrainLight golden = LOW_EVENING_SUN.withGolden(1);
    assertEquals(1, golden.edgeGlow(Edge.LEFT), 1e-9);
    assertEquals(0, golden.edgeGlow(Edge.RIGHT), 1e-9);
    assertEquals(0, golden.edgeGlow(Edge.BACK), 1e-9);
  }

  @Test
  void edgesFacingDownCatchAQuarterOfTheGlowWhereverTheSunIs() {
    assertEquals(0.25, LOW_EVENING_SUN.withGolden(1).edgeGlow(Edge.FRONT), 1e-9);
    assertEquals(0.2, LOW_EVENING_SUN.withGolden(0.8).edgeGlow(Edge.FRONT), 1e-9);
    TerrainLight morning = new TerrainLight(-90, 0, 0.15, 0.15, true, 0, 0).withGolden(1);
    assertEquals(0.25, morning.edgeGlow(Edge.FRONT), 1e-9);
  }

  @Test
  void anEdgeFacingDownAndTowardTheSunGlowsFully() {
    // A sun low in the south, shadows falling north.
    TerrainLight southern = new TerrainLight(0, 90, 0.15, 0.15, true, 0, 0).withGolden(1);
    assertEquals(1, southern.edgeGlow(Edge.FRONT), 1e-9);
  }

  @Test
  void theGlowDoesNotFadeWithTheWeakLightOfALowSun() {
    TerrainLight golden = LOW_EVENING_SUN.withGolden(0.8);
    assertEquals(0.15, golden.edgeLight(Edge.LEFT), 1e-9);
    assertEquals(0.8, golden.edgeGlow(Edge.LEFT), 1e-9);
  }

  @Test
  void withoutGoldenHourNothingGlows() {
    assertEquals(0, LOW_EVENING_SUN.golden());
    for (Edge edge : Edge.values()) {
      assertEquals(0, LOW_EVENING_SUN.edgeGlow(edge));
      assertEquals(0, TerrainLight.DEFAULT.edgeGlow(edge));
    }
  }

  @Test
  void cloudDimsTheGlowAndLightningHasNone() {
    assertEquals(0.25, LOW_EVENING_SUN.withGolden(1).overcast(0.75).golden(), 1e-9);
    assertEquals(0, TerrainLight.lightningStrike(1.0).golden());
  }

  @Test
  void goldenMustBeBetweenZeroAndOne() {
    assertThrows(IllegalArgumentException.class, () -> LOW_EVENING_SUN.withGolden(1.5));
    assertThrows(IllegalArgumentException.class, () -> LOW_EVENING_SUN.withGolden(-0.1));
  }
}
