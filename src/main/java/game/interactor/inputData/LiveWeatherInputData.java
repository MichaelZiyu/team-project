package game.interactor.inputData;

/**
 * What the player chose for live weather: none, a city's, or wherever they seem to be.
 *
 * @param source where the live weather comes from
 * @param city the city's name, for {@link Source#CITY}; otherwise empty
 */
public record LiveWeatherInputData(Source source, String city) {

  /** Where live weather comes from. */
  public enum Source {
    /** No live weather: the game keeps its own. */
    OFF,
    /** The weather in a city the player names. */
    CITY,
    /** The weather where the player seems to be, guessed from their internet address. */
    NEARBY
  }

  /**
   * Creates the choice.
   *
   * @throws NullPointerException if {@code source} is null
   */
  public LiveWeatherInputData {
    if (source == null) {
      throw new NullPointerException("source");
    }
    city = city == null ? "" : city.strip();
  }

  /**
   * Returns the choice of no live weather.
   *
   * @return live weather turned off
   */
  public static LiveWeatherInputData off() {
    return new LiveWeatherInputData(Source.OFF, "");
  }

  /**
   * Returns the choice of a city's weather.
   *
   * @param city the city's name, as the player typed it
   * @return live weather from that city
   */
  public static LiveWeatherInputData city(String city) {
    return new LiveWeatherInputData(Source.CITY, city);
  }

  /**
   * Returns the choice of the weather where the player seems to be.
   *
   * @return live weather from the player's guessed location
   */
  public static LiveWeatherInputData nearby() {
    return new LiveWeatherInputData(Source.NEARBY, "");
  }
}
