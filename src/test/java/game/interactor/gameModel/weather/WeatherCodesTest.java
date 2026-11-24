package game.interactor.gameModel.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class WeatherCodesTest {

  private static void assertCodes(Weather expected, int... codes) {
    for (int code : codes) {
      assertEquals(Optional.of(expected), WeatherCodes.weatherFor(code), "code " + code);
      assertTrue(WeatherCodes.describe(code).isPresent(), "description of " + code);
    }
  }

  @Test
  void clearSkiesAreClear() {
    assertCodes(Weather.CLEAR, 0, 1);
  }

  @Test
  void partlyCloudyAndOvercastAreCloudy() {
    assertCodes(Weather.CLOUDY, 2, 3);
  }

  @Test
  void fogIsFog() {
    assertCodes(Weather.FOG, 45, 48);
  }

  @Test
  void everyKindOfDrizzleAndRainIsRain() {
    assertCodes(Weather.RAIN, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82);
  }

  @Test
  void lightAndModerateSnowIsSnow() {
    assertCodes(Weather.SNOW, 71, 73, 77, 85);
  }

  @Test
  void heavySnowIsBlizzard() {
    assertCodes(Weather.BLIZZARD, 75, 86);
  }

  @Test
  void everyThunderstormStaysThunderstorm() {
    assertCodes(Weather.THUNDERSTORM, 95, 96, 97, 99);
  }

  @Test
  void anUnknownCodeHasNoWeather() {
    assertEquals(Optional.empty(), WeatherCodes.weatherFor(4));
    assertEquals(Optional.empty(), WeatherCodes.describe(-1));
  }

  @Test
  void descriptionsSayWhatTheCodeMeans() {
    assertEquals(Optional.of("Overcast"), WeatherCodes.describe(3));
    assertEquals(Optional.of("Heavy snowfall"), WeatherCodes.describe(75));
  }
}
