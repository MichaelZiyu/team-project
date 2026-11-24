package game.interactor.gameModel.weather;

import java.util.Map;
import java.util.Optional;

/**
 * The game's weather for real weather, as described by the World Meteorological Organization's
 * weather codes, which weather services such as Open-Meteo report.
 *
 * <p>Each code becomes the nearest weather the game has:
 *
 * <ul>
 *   <li>clear or mainly clear (0, 1): {@link Weather#CLEAR};
 *   <li>partly cloudy or overcast (2, 3): {@link Weather#CLOUDY};
 *   <li>fog (45, 48): {@link Weather#FOG};
 *   <li>any drizzle or rain, freezing or not, and rain showers (51 to 67, 80 to 82): {@link
 *       Weather#RAIN};
 *   <li>light or moderate snow, snow grains and light snow showers (71, 73, 77, 85): {@link
 *       Weather#SNOW};
 *   <li>heavy snow and heavy snow showers (75, 86): {@link Weather#BLIZZARD};
 *   <li>any thunderstorm, with or without hail (95, 96, 97, 99): {@link Weather#THUNDERSTORM}.
 * </ul>
 */
public final class WeatherCodes {

  /** Each code's weather and description. */
  private static final Map<Integer, Entry> CODES =
      Map.ofEntries(
          entry(0, Weather.CLEAR, "Clear sky"),
          entry(1, Weather.CLEAR, "Mainly clear"),
          entry(2, Weather.CLOUDY, "Partly cloudy"),
          entry(3, Weather.CLOUDY, "Overcast"),
          entry(45, Weather.FOG, "Fog"),
          entry(48, Weather.FOG, "Depositing rime fog"),
          entry(51, Weather.RAIN, "Light drizzle"),
          entry(53, Weather.RAIN, "Moderate drizzle"),
          entry(55, Weather.RAIN, "Dense drizzle"),
          entry(56, Weather.RAIN, "Light freezing drizzle"),
          entry(57, Weather.RAIN, "Dense freezing drizzle"),
          entry(61, Weather.RAIN, "Slight rain"),
          entry(63, Weather.RAIN, "Moderate rain"),
          entry(65, Weather.RAIN, "Heavy rain"),
          entry(66, Weather.RAIN, "Light freezing rain"),
          entry(67, Weather.RAIN, "Heavy freezing rain"),
          entry(71, Weather.SNOW, "Slight snowfall"),
          entry(73, Weather.SNOW, "Moderate snowfall"),
          entry(75, Weather.BLIZZARD, "Heavy snowfall"),
          entry(77, Weather.SNOW, "Snow grains"),
          entry(80, Weather.RAIN, "Slight rain showers"),
          entry(81, Weather.RAIN, "Moderate rain showers"),
          entry(82, Weather.RAIN, "Violent rain showers"),
          entry(85, Weather.SNOW, "Slight snow showers"),
          entry(86, Weather.BLIZZARD, "Heavy snow showers"),
          entry(95, Weather.THUNDERSTORM, "Thunderstorm"),
          entry(96, Weather.THUNDERSTORM, "Thunderstorm with slight hail"),
          entry(97, Weather.THUNDERSTORM, "Heavy thunderstorm"),
          entry(99, Weather.THUNDERSTORM, "Thunderstorm with heavy hail"));

  /** A code's weather and what the code means. */
  private record Entry(Weather weather, String description) {}

  private WeatherCodes() {}

  /**
   * Returns the game's weather for a weather code.
   *
   * @param code a WMO weather code
   * @return the nearest weather the game has; empty for a code that is not known
   */
  public static Optional<Weather> weatherFor(int code) {
    return Optional.ofNullable(CODES.get(code)).map(Entry::weather);
  }

  /**
   * Returns what a weather code means, in words.
   *
   * @param code a WMO weather code
   * @return its description, such as "Overcast"; empty for a code that is not known
   */
  public static Optional<String> describe(int code) {
    return Optional.ofNullable(CODES.get(code)).map(Entry::description);
  }

  private static Map.Entry<Integer, Entry> entry(int code, Weather weather, String description) {
    return Map.entry(code, new Entry(weather, description));
  }
}
