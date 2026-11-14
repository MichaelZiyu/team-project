package weatherscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.gameModel.Renderable;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import weather.Weather;

class WeatherLayerTest {

  private static final double FRAME = 1.0 / 60;

  private static List<Renderable> contents(WeatherLayer layer) {
    List<Renderable> contents = new ArrayList<>();
    layer.forEach(contents::add);
    return contents;
  }

  /** A layer with its vanishing point on screen, so the number of drops is the style's. */
  private static WeatherLayer layer() {
    WeatherLayer layer = new WeatherLayer(800, 600, 1);
    layer.perspective().setVanishingOffset(0.12);
    layer.perspective().setOvalWidth(0);
    return layer;
  }

  private static void run(WeatherLayer layer, double seconds) {
    for (double t = 0; t < seconds; t += FRAME) {
      layer.update(FRAME, 1234.6, -567.2);
    }
  }

  @Test
  void clearWeatherDrawsNothing() {
    WeatherLayer layer = new WeatherLayer(800, 600, 1);
    run(layer, 1);
    assertTrue(contents(layer).isEmpty());
  }

  @Test
  void rainCoversTheWholeScreen() {
    WeatherLayer layer = layer();
    layer.setWeather(Weather.RAIN);
    run(layer, 2);
    List<Renderable> contents = contents(layer);
    assertEquals(1, contents.size());
    Renderable overlay = contents.get(0);
    // The game camera's rule, for a player at whole pixel (1234, -567).
    int screenX = overlay.getX() - 1234 + 400 - overlay.getWidth() / 2;
    int screenY = 300 - (overlay.getY() - (-567)) - overlay.getHeight();
    assertTrue(screenX <= 0 && screenY <= 0);
    assertTrue(screenX + overlay.getWidth() >= 800 && screenY + overlay.getHeight() >= 600);
    assertEquals(RainStyle.RAIN.drops(), layer.dropCount());
  }

  @Test
  void thunderstormIsDarkerThanRain() {
    WeatherLayer rain = new WeatherLayer(800, 600, 1);
    rain.setWeather(Weather.RAIN);
    WeatherLayer storm = new WeatherLayer(800, 600, 1);
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
    double darkBefore = averageAlpha(layer.image());
    double brightBefore = averageBrightness(layer.image());
    layer.strikeLightning();
    layer.update(0, 1234.6, -567.2);
    // Less of the scene is covered, so the terrain shows brighter, and what does cover it is a
    // faint light rather than a white wash.
    assertTrue(averageBrightness(layer.image()) > brightBefore + 50, "a bright flash");
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
  void rainFadesAwayWhenTheSkyClears() {
    WeatherLayer layer = new WeatherLayer(800, 600, 1);
    layer.setWeather(Weather.THUNDERSTORM);
    run(layer, 2);
    layer.setWeather(Weather.CLEAR);
    run(layer, 4);
    assertTrue(contents(layer).isEmpty());
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
}
