package game.interactor.dataAccess;

/**
 * Finds places and the real weather there, for live weather.
 *
 * <p>Each method may wait on the internet.
 */
public interface LiveWeatherDataAccessInterface {

  /**
   * A place on Earth.
   *
   * @param name its name for the player, such as "Toronto, Canada"
   * @param latitude degrees north of the equator; negative is south
   * @param longitude degrees east of Greenwich; negative is west
   */
  record Place(String name, double latitude, double longitude) {}

  /**
   * Finds a city by name.
   *
   * @param city the city's name
   * @return the best match
   * @throws LiveWeatherException if there is no such city, or it cannot be looked up
   */
  Place findCity(String city) throws LiveWeatherException;

  /**
   * Guesses roughly where the player is, from their internet address.
   *
   * @return the place, usually a nearby city
   * @throws LiveWeatherException if it cannot be guessed
   */
  Place guessLocation() throws LiveWeatherException;

  /**
   * Returns the weather at a place right now.
   *
   * @param place the place
   * @return its WMO weather code (see {@link game.interactor.gameModel.weather.WeatherCodes})
   * @throws LiveWeatherException if it cannot be found
   */
  int currentWeatherCode(Place place) throws LiveWeatherException;
}
