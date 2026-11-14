package weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WeatherTest {

  @Test
  void clearSkiesBlockNothingAndKeepFullColour() {
    assertEquals(0, Weather.CLEAR.cloudCover());
    assertEquals(1, Weather.CLEAR.saturation());
  }

  @Test
  void stormsAreDarkerAndGreyerThanRain() {
    assertTrue(Weather.THUNDERSTORM.cloudCover() >= Weather.RAIN.cloudCover());
    assertTrue(Weather.THUNDERSTORM.saturation() < Weather.RAIN.saturation());
    assertTrue(Weather.BLIZZARD.saturation() < Weather.SNOW.saturation());
  }

  @Test
  void everyWeatherHasValuesInRange() {
    for (Weather weather : Weather.values()) {
      assertTrue(weather.cloudCover() >= 0 && weather.cloudCover() <= 1, weather.name());
      assertTrue(weather.saturation() >= 0 && weather.saturation() <= 1, weather.name());
    }
  }

  @Test
  void rainLeavesTheGroundWithoutSunAndGreyer() {
    assertTrue(Weather.RAIN.cloudCover() > 0.5, "sunlit rims go dark in rain");
    assertTrue(Weather.RAIN.saturation() < 0.75, "colours are noticeably greyer in rain");
  }
}
