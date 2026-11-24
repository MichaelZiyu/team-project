package game.interactor.gameModel.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class WeatherLayerFogTest {

  private static final int VIEW_WIDTH = 400;
  private static final int VIEW_HEIGHT = 300;

  /** A layer with fog fully rolled in, around the camera's start. */
  private static WeatherLayer foggy(double sunlight) {
    WeatherLayer layer = new WeatherLayer(VIEW_WIDTH, VIEW_HEIGHT, 11);
    layer.setSunlight(sunlight);
    layer.setWeather(Weather.FOG);
    for (int i = 0; i < 300; i++) {
      layer.update(1.0 / 60, 0, 0);
    }
    return layer;
  }

  private static int alpha(int argb) {
    return argb >>> 24;
  }

  @Test
  void fogRollsInGraduallyAndClearsAgain() {
    WeatherLayer layer = new WeatherLayer(VIEW_WIDTH, VIEW_HEIGHT, 11);
    layer.setWeather(Weather.FOG);
    layer.update(1, 0, 0);
    assertTrue(layer.fogAmount() > 0 && layer.fogAmount() < 1);
    for (int i = 0; i < 10; i++) {
      layer.update(1, 0, 0);
    }
    assertEquals(1, layer.fogAmount());
    layer.setWeather(Weather.CLEAR);
    for (int i = 0; i < 10; i++) {
      layer.update(1, 0, 0);
    }
    assertEquals(0, layer.fogAmount());
    assertFalse(layer.isShowing());
    assertNull(layer.paintFrame(0, 0));
  }

  @Test
  void fogIsThickInSomePlacesAndThinInOthers() {
    BufferedImage image = foggy(1).paintFrame(0, 0);
    int least = 255;
    int most = 0;
    for (int y = 0; y < image.getHeight(); y += 4) {
      for (int x = 0; x < image.getWidth(); x += 4) {
        int a = alpha(image.getRGB(x, y));
        least = Math.min(least, a);
        most = Math.max(most, a);
      }
    }
    assertTrue(least < 90, "thinnest fog's opacity " + least);
    assertTrue(most > 170, "thickest fog's opacity " + most);
  }

  @Test
  void fogStaysPutInTheWorldAsTheCameraMoves() {
    WeatherLayer layer = foggy(1);
    int before = layer.paintFrame(0, 0).getRGB(200, 150);
    // The camera moves 30 pixels right; no time passes, so the fog does not drift.
    assertEquals(before, layer.paintFrame(30, 0).getRGB(170, 150));
  }

  @Test
  void moreFogIsThicker() {
    WeatherLayer usual = foggy(1);
    WeatherLayer thick = foggy(1);
    thick.setRainAmount(2.5);
    assertTrue(
        alpha(thick.paintFrame(0, 0).getRGB(200, 150))
            > alpha(usual.paintFrame(0, 0).getRGB(200, 150)),
        "thicker at the same place");
  }

  @Test
  void fogIsDarkAtNight() {
    int dayRed = foggy(1).paintFrame(0, 0).getRGB(200, 150) >> 16 & 0xFF;
    int nightRed = foggy(0).paintFrame(0, 0).getRGB(200, 150) >> 16 & 0xFF;
    assertTrue(nightRed < dayRed / 2, "night " + nightRed + ", day " + dayRed);
  }

  @Test
  void cloudGreysTheSkyAndDimsTheSunWithoutRainOrSnow() {
    WeatherLayer layer = new WeatherLayer(VIEW_WIDTH, VIEW_HEIGHT, 11);
    layer.setWeather(Weather.CLOUDY);
    for (int i = 0; i < 300; i++) {
      layer.update(1.0 / 60, 0, 0);
    }
    assertEquals(Weather.CLOUDY.cloudCover(), layer.cloudCover(), 1e-9);
    BufferedImage image = layer.paintFrame(0, 0);
    // The same soft grey everywhere: no falling rain or snow, and no fog banks.
    int corner = image.getRGB(0, 0);
    assertEquals(WeatherLayer.CLOUD_TINT.getAlpha(), alpha(corner));
    assertEquals(corner, image.getRGB(200, 150));
    assertEquals(corner, image.getRGB(399, 299));
  }
}
