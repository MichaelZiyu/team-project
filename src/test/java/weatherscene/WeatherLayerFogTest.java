package weatherscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import weather.Weather;

class WeatherLayerFogTest {

  private static final int VIEW_WIDTH = 400;
  private static final int VIEW_HEIGHT = 300;

  /** A layer with fog fully rolled in, at the camera's start. */
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
    assertFalse(layer.iterator().hasNext(), "nothing left to draw once the fog has cleared");
  }

  @Test
  void fogIsThickInSomePlacesAndThinInOthers() {
    BufferedImage image = foggy(1).image();
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
    BufferedImage image = layer.image();
    int before = image.getRGB(200, 150);
    // No time passes, so the fog does not drift: only the camera moves, 30 pixels right.
    layer.update(0, 30, 0);
    assertEquals(before, image.getRGB(170, 150));
  }

  @Test
  void moreFogIsThicker() {
    WeatherLayer usual = foggy(1);
    WeatherLayer thick = foggy(1);
    thick.setRainAmount(2.5);
    thick.update(0, 0, 0);
    assertTrue(
        alpha(thick.image().getRGB(200, 150)) > alpha(usual.image().getRGB(200, 150)),
        "thicker at the same place");
  }

  @Test
  void fogIsDarkAtNight() {
    BufferedImage day = foggy(1).image();
    BufferedImage night = foggy(0).image();
    int x = 200;
    int y = 150;
    int dayRed = day.getRGB(x, y) >> 16 & 0xFF;
    int nightRed = night.getRGB(x, y) >> 16 & 0xFF;
    assertTrue(nightRed < dayRed / 2, "night " + nightRed + ", day " + dayRed);
    assertTrue(nightRed < 120);
  }
}
