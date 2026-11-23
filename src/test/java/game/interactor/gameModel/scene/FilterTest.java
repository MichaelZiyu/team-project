package game.interactor.gameModel.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.interactor.gameModel.weather.Weather;
import game.interactor.gameModel.weather.WeatherSound;
import game.interactor.outputData.RenderableData.Layer;
import game.settings.GameSettings;
import java.util.List;
import org.junit.jupiter.api.Test;

class FilterTest {

  private static final double NOON = 0.5;
  private static final double MIDNIGHT = 0;

  /** A filter whose lightning only comes when a test strikes on purpose. */
  private static Filter filter() {
    Filter filter = new Filter(1);
    filter.weatherLayer().setStrikeReady(false);
    return filter;
  }

  private static void run(Filter filter, double seconds) {
    for (int i = 0; i < seconds * GameSettings.FPS; i++) {
      filter.tick(null);
      filter.layer(0, 0, List.of());
    }
  }

  @Test
  void eachTickMovesTheDayOnAndADayLastsTenMinutes() {
    Filter filter = filter();
    double start = filter.clock().timeOfDay();
    filter.tick(null);
    assertEquals(1.0 / filter.clock().ticksPerDay(), filter.clock().timeOfDay() - start, 1e-12);
    assertEquals(
        Math.round(Filter.DAY_SECONDS * GameSettings.FPS), filter.clock().ticksPerDay());
  }

  @Test
  void aClearMiddayHasNothingToLayOverTheView() {
    Filter filter = filter();
    filter.clock().setTimeOfDay(NOON);
    Layer layer = filter.layer(0, 0, List.of());
    assertTrue(layer.screenFixed());
    assertTrue(layer.items().isEmpty());
    assertEquals(1, filter.sun().daylight(), 1e-9);
  }

  @Test
  void nightTintsTheWholeViewAndReusesItsImage() {
    Filter filter = filter();
    filter.clock().setTimeOfDay(MIDNIGHT);
    Layer first = filter.layer(0, 0, List.of());
    assertEquals(1, first.items().size());
    assertEquals(0, first.items().get(0).x());
    assertEquals(0, first.items().get(0).y());
    assertEquals(Filter.DEFAULT_VIEW_WIDTH, first.items().get(0).texture().getWidth());
    Layer again = filter.layer(10, 20, List.of());
    assertSame(first.items().get(0).texture(), again.items().get(0).texture(), "same colour");
    filter.clock().setTimeOfDay(0.27);
    Layer dawn = filter.layer(0, 0, List.of());
    assertNotSame(first.items().get(0).texture(), dawn.items().get(0).texture(), "new colour");
  }

  @Test
  void rainIsLaidOverTheViewAndHeard() {
    Filter filter = filter();
    filter.clock().setTimeOfDay(NOON);
    filter.setWeather(Weather.RAIN);
    run(filter, 3);
    Layer layer = filter.layer(0, 0, List.of());
    assertEquals(1, layer.items().size(), "no tint at noon, just the weather");
    assertEquals(Weather.RAIN, filter.getWeather());
    assertEquals(WeatherSound.LIGHT_RAIN, filter.loopingSounds().get(0).name());
    assertEquals(Weather.RAIN.cloudCover(), filter.cloudCover(), 1e-9);
  }

  @Test
  void lightningIsFollowedByThunder() {
    Filter filter = filter();
    filter.setWeather(Weather.THUNDERSTORM);
    run(filter, 1);
    filter.weatherLayer().strikeLightning();
    run(filter, 4.2);
    // The thunder was heard at most 4 seconds after the flash, and has since left the frames.
    assertTrue(filter.soundEvents().isEmpty());
    filter.weatherLayer().strikeLightning();
    boolean heard = false;
    for (int i = 0; i < 4.2 * GameSettings.FPS && !heard; i++) {
      filter.tick(null);
      heard = !filter.soundEvents().isEmpty();
    }
    assertTrue(heard, "thunder after the flash");
  }

  @Test
  void aNewViewSizeKeepsTheWeather() {
    Filter filter = filter();
    filter.setWeather(Weather.SNOW);
    filter.setViewSize(1280, 720);
    filter.clock().setTimeOfDay(MIDNIGHT);
    run(filter, 2);
    Layer layer = filter.layer(0, 0, List.of());
    assertEquals(2, layer.items().size(), "the night's tint and the snow");
    for (var item : layer.items()) {
      assertEquals(1280, item.texture().getWidth());
      assertEquals(720, item.texture().getHeight());
    }
    assertEquals(Weather.SNOW, filter.getWeather());
    assertEquals(Weather.SNOW, filter.weatherLayer().getWeather());
  }
}
