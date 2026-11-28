package game.interactor.gameModel.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.interactor.gameModel.Texture;
import game.interactor.outputData.RenderableData.RenderableItem;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WeatherLayerTest {

  private static final double FRAME = 1.0 / 60;

  /**
   * A layer with its vanishing point on screen, so the number of drops is the style's, and with
   * lightning held back, so flashes only come when a test strikes on purpose.
   */
  private static WeatherLayer layer() {
    WeatherLayer layer = new WeatherLayer(800, 600, 1);
    layer.perspective().setVanishingOffset(0.12);
    layer.perspective().setOvalWidth(0);
    layer.setStrikeReady(false);
    return layer;
  }

  /** Moves the weather on by a tick and paints a frame, as the game and renderer do. */
  private static void step(WeatherLayer layer, double seconds) {
    layer.update(seconds, 1234.6, -567.2);
    layer.paintFrame(1234.6, -567.2);
  }

  private static void run(WeatherLayer layer, double seconds) {
    for (double t = 0; t < seconds; t += FRAME) {
      step(layer, FRAME);
    }
  }

  @Test
  void clearWeatherDrawsNothing() {
    WeatherLayer layer = new WeatherLayer(800, 600, 1);
    run(layer, 1);
    assertFalse(layer.isShowing());
    assertNull(layer.paintFrame(1234.6, -567.2));
  }

  @Test
  void rainIsPaintedOverTheWholeScreen() {
    WeatherLayer layer = layer();
    layer.setWeather(Weather.RAIN);
    run(layer, 2);
    BufferedImage image = layer.paintFrame(1234.6, -567.2);
    assertEquals(800, image.getWidth());
    assertEquals(600, image.getHeight());
    assertEquals(RainStyle.RAIN.drops(), layer.dropCount());
  }

  @Test
  void framesTakeTurnsBetweenImagesSoAHandedOverOneIsNotPaintedOver() {
    WeatherLayer layer = layer();
    layer.setWeather(Weather.RAIN);
    run(layer, 1);
    List<BufferedImage> painted = new ArrayList<>();
    for (int i = 0; i <= WeatherLayer.FRAME_IMAGES; i++) {
      painted.add(layer.paintFrame(0, 0));
    }
    for (int i = 0; i < WeatherLayer.FRAME_IMAGES; i++) {
      for (int j = i + 1; j < WeatherLayer.FRAME_IMAGES; j++) {
        assertNotSame(painted.get(i), painted.get(j), "frames in a row use different images");
      }
    }
    assertSame(painted.get(0), painted.get(WeatherLayer.FRAME_IMAGES), "then it comes round");
  }

  @Test
  void thunderstormIsDarkerThanRain() {
    WeatherLayer rain = layer();
    rain.setWeather(Weather.RAIN);
    WeatherLayer storm = layer();
    storm.setWeather(Weather.THUNDERSTORM);
    run(rain, 3);
    run(storm, 3);
    assertTrue(averageAlpha(storm.image()) > averageAlpha(rain.image()));
  }

  @Test
  void lightningLiftsTheStormsDarknessInsteadOfWhitingOut() {
    WeatherLayer layer = layer();
    layer.setWeather(Weather.THUNDERSTORM);
    run(layer, 3);
    double brightBefore = averageBrightness(layer.image());
    double strength = layer.upcomingStrikeIntensity();
    layer.strikeLightning();
    step(layer, 0);
    // Less of the scene is covered, so the terrain shows brighter, and what does cover it is a
    // faint light rather than a white wash. Weaker strikes brighten it less.
    assertTrue(averageBrightness(layer.image()) > brightBefore + 40 * strength, "a bright flash");
    assertTrue(averageAlpha(layer.image()) < 0.5 * 255, "the terrain still shows through");
  }

  @Test
  void rainAmountAndSpeedAdjustTheWeathersUsualRain() {
    WeatherLayer layer = layer();
    layer.setWeather(Weather.RAIN);
    layer.setRainAmount(0.5);
    run(layer, 3);
    assertEquals(Math.round(RainStyle.RAIN.drops() * 0.5), layer.dropCount());
    layer.setWeather(Weather.THUNDERSTORM);
    run(layer, 3);
    assertEquals(RainStyle.THUNDERSTORM.drops() / 2, layer.dropCount(), "amount carries over");
    layer.setRainAmount(0);
    run(layer, 3);
    assertEquals(0, layer.dropCount());
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class, () -> layer.setRainSpeed(0));
  }

  @Test
  void rainIsBrightInSunlightAndDimAtNightLikeASunFacingRim() {
    WeatherLayer layer = layer();
    assertEquals(WeatherLayer.RAIN_COLOUR.getRGB() & 0xFFFFFF,
        layer.brightestStreak().getRGB() & 0xFFFFFF, "full daylight by default");
    layer.setSunlight(0);
    assertEquals(WeatherLayer.RAIN_COLOUR_DARK.getRGB() & 0xFFFFFF,
        layer.brightestStreak().getRGB() & 0xFFFFFF);
    layer.setSunlight(0.5);
    int halfway = layer.brightestStreak().getRed();
    assertTrue(Math.abs(halfway - (70 + 190) / 2) <= 2, "blends halfway: " + halfway);
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class, () -> layer.setSunlight(1.5));
  }

  @Test
  void lightningLightsUpRainAtNight() {
    WeatherLayer layer = layer();
    layer.setWeather(Weather.THUNDERSTORM);
    layer.setSunlight(0);
    run(layer, 1);
    int dark = layer.brightestStreak().getRed();
    layer.strikeLightning();
    layer.update(0, 0, 0);
    assertTrue(layer.brightestStreak().getRed() > dark + 50, "the flash lights the rain");
  }

  @Test
  void cloudCoverFadesInAndOutWithTheWeather() {
    WeatherLayer layer = layer();
    assertEquals(0, layer.cloudCover());
    layer.setWeather(Weather.RAIN);
    run(layer, 3);
    assertEquals(Weather.RAIN.cloudCover(), layer.cloudCover(), 1e-9);
    layer.setWeather(Weather.CLEAR);
    layer.update(0.3, 0, 0);
    double fading = layer.cloudCover();
    assertTrue(fading > 0 && fading < Weather.RAIN.cloudCover(), "fading out: " + fading);
    run(layer, 3);
    assertEquals(0, layer.cloudCover(), 1e-9);
  }

  @Test
  void withTerrainLightningTheWhiteWashIsLeftOutAtTheBrightMoments() {
    WeatherLayer withTerrain = layer();
    WeatherLayer without = layer();
    withTerrain.setTerrainLightning(true);
    for (WeatherLayer layer : new WeatherLayer[] {withTerrain, without}) {
      layer.setWeather(Weather.THUNDERSTORM);
      run(layer, 3);
      layer.strikeLightning();
      step(layer, 0);
    }
    assertTrue(withTerrain.isStrikeBright());
    assertTrue(
        averageBrightness(withTerrain.image()) + 20 < averageBrightness(without.image()),
        "no white wash over the terrain's own strike lighting");
  }

  @Test
  void charactersHideTheSplashesUnderThem() {
    WeatherLayer layer = layer();
    layer.update(0, 1000, 500);
    // A 48 x 64 character standing on the camera's spot: the renderer centres it on x and puts its
    // bottom at y, so on screen it covers x 376..424 and y 236..300.
    Texture sprite = new Texture(new BufferedImage(48, 64, BufferedImage.TYPE_INT_ARGB), 48, 64);
    layer.setSplashBlockers(List.of(new RenderableItem(null, 1000, 500)));
    assertTrue(!layer.isBlocked(400, 280), "a character with no image is not drawn");
    layer.setSplashBlockers(List.of(new RenderableItem(sprite, 1000, 500)));
    assertTrue(layer.isBlocked(400, 280));
    assertTrue(!layer.isBlocked(400, 320), "below the character");
    assertTrue(!layer.isBlocked(440, 280), "beside the character");
  }

  @Test
  void rainFadesAwayWhenTheSkyClears() {
    WeatherLayer layer = new WeatherLayer(800, 600, 1);
    layer.setWeather(Weather.THUNDERSTORM);
    run(layer, 2);
    layer.setWeather(Weather.CLEAR);
    run(layer, 4);
    assertFalse(layer.isShowing());
    assertEquals(0, layer.dropCount());
  }

  private static double averageAlpha(BufferedImage image) {
    long sum = 0;
    for (int x = 0; x < image.getWidth(); x += 7) {
      for (int y = 0; y < image.getHeight(); y += 7) {
        sum += image.getRGB(x, y) >>> 24;
      }
    }
    return (double) sum / ((image.getWidth() / 7 + 1) * (image.getHeight() / 7 + 1));
  }

  private static double averageBrightness(BufferedImage image) {
    long sum = 0;
    int count = 0;
    for (int x = 0; x < image.getWidth(); x += 7) {
      for (int y = 0; y < image.getHeight(); y += 7) {
        int argb = image.getRGB(x, y);
        int alpha = argb >>> 24;
        sum += (((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF)) / 3 * alpha / 255;
        count++;
      }
    }
    return (double) sum / count;
  }

  @Test
  void snowFallsAsSolidPalePixelFlakes() {
    WeatherLayer layer = layer();
    layer.setWeather(Weather.SNOW);
    run(layer, 6);
    assertTrue(layer.flakeCount() > 0);
    assertEquals(0, layer.dropCount(), "snow is not rain");
    BufferedImage image = layer.image();
    int flakePixels = 0;
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        int argb = image.getRGB(x, y);
        if ((argb >>> 24) > 120 && (argb & 0xFF) > 200 && ((argb >> 16) & 0xFF) > 200) {
          flakePixels++;
        }
      }
    }
    assertTrue(flakePixels > 500, "flakes should show as bright pixels: " + flakePixels);
  }

  @Test
  void blizzardsHaveFarMoreFlakesThanSnow() {
    WeatherLayer snow = layer();
    snow.setWeather(Weather.SNOW);
    WeatherLayer blizzard = layer();
    blizzard.setWeather(Weather.BLIZZARD);
    run(snow, 5);
    run(blizzard, 5);
    assertTrue(blizzard.flakeCount() > 3 * snow.flakeCount());
    assertEquals(Weather.BLIZZARD.cloudCover(), blizzard.cloudCover(), 1e-9);
  }

  @Test
  void snowStopsWhenTheSkyClears() {
    WeatherLayer layer = layer();
    layer.setWeather(Weather.SNOW);
    run(layer, 5);
    layer.setWeather(Weather.CLEAR);
    run(layer, 30);
    assertEquals(0, layer.flakeCount());
    assertFalse(layer.isShowing());
  }

  @Test
  void flakesAreLitBySunlightAndGrowTowardTheCamera() {
    WeatherLayer layer = layer();
    layer.setSunlight(1);
    assertEquals(
        WeatherLayer.SNOW_COLOUR.getRGB() & 0xFFFFFF, layer.nearestFlake().getRGB() & 0xFFFFFF);
    layer.setSunlight(0);
    assertEquals(
        WeatherLayer.SNOW_COLOUR_DARK.getRGB() & 0xFFFFFF,
        layer.nearestFlake().getRGB() & 0xFFFFFF);
    assertEquals(WeatherLayer.MIN_FLAKE_PIXEL_PX, WeatherLayer.flakePixel(0));
    assertEquals(WeatherLayer.MAX_FLAKE_PIXEL_PX, WeatherLayer.flakePixel(1));
  }

  @Test
  void eachLightningStrikeIsAnnouncedOnceWithItsStrength() {
    WeatherLayer layer = layer();
    layer.setWeather(Weather.THUNDERSTORM);
    List<Double> heard = new ArrayList<>();
    layer.setLightningListener(heard::add);
    run(layer, 1);
    assertTrue(heard.isEmpty(), "no strike yet");
    double strength = layer.upcomingStrikeIntensity();
    layer.strikeLightning();
    run(layer, 1);
    assertEquals(List.of(strength), heard);
  }
}
