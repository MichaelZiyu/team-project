package game.interactor;

import game.interactor.dataAccess.LiveWeatherDataAccessInterface;
import game.interactor.dataAccess.LiveWeatherDataAccessInterface.Place;
import game.interactor.dataAccess.LiveWeatherException;
import game.interactor.gameModel.scene.Filter;
import game.interactor.gameModel.weather.Weather;
import game.interactor.gameModel.weather.WeatherCodes;
import game.interactor.inputBoundaries.LiveWeatherInputBoundary;
import game.interactor.inputData.LiveWeatherInputData;
import game.interactor.outputBoundaries.LiveWeatherOutputBoundary;
import game.interactor.outputData.LiveWeatherOutputData;
import java.util.Optional;

/**
 * Matches the game's weather to the real weather somewhere: in a city the player names, or where
 * they seem to be.
 *
 * <p>Choosing a source finds its place once, then each refresh looks up the weather there and
 * sets the game's to match ({@link WeatherCodes}). Turning live weather off leaves the game's
 * weather as it is. If anything cannot be found, the player is told why and the game keeps the
 * weather it had.
 */
public final class LiveWeatherInteractor implements LiveWeatherInputBoundary {

  /** The credit the weather data's licence (CC BY 4.0) asks to be shown with it. */
  public static final String ATTRIBUTION = "Weather data by Open-Meteo.com";

  private final LiveWeatherDataAccessInterface dataAccess;
  private final LiveWeatherOutputBoundary presenter;
  private final Filter filter;

  /** Where live weather comes from; null while it is off. */
  private Place place;

  /**
   * Creates the use case.
   *
   * @param dataAccess finds places and their weather
   * @param presenter tells the player what live weather is doing
   * @param filter the scene whose weather follows the live weather
   */
  public LiveWeatherInteractor(
      LiveWeatherDataAccessInterface dataAccess,
      LiveWeatherOutputBoundary presenter,
      Filter filter) {
    this.dataAccess = dataAccess;
    this.presenter = presenter;
    this.filter = filter;
  }

  @Override
  public void changeLiveWeather(LiveWeatherInputData input) {
    place = null;
    try {
      switch (input.source()) {
        case CITY:
          if (input.city().isEmpty()) {
            presenter.presentLiveWeatherFailure("Type the name of a city.");
            return;
          }
          place = dataAccess.findCity(input.city());
          break;
        case NEARBY:
          place = dataAccess.guessLocation();
          break;
        case OFF:
        default:
          presenter.presentLiveWeatherOff();
          return;
      }
    } catch (LiveWeatherException e) {
      presenter.presentLiveWeatherFailure(e.getMessage());
      return;
    }
    refreshLiveWeather();
  }

  @Override
  public void refreshLiveWeather() {
    if (place == null) {
      return;
    }
    int code;
    try {
      code = dataAccess.currentWeatherCode(place);
    } catch (LiveWeatherException e) {
      presenter.presentLiveWeatherFailure(e.getMessage());
      return;
    }
    Optional<Weather> weather = WeatherCodes.weatherFor(code);
    if (weather.isEmpty()) {
      presenter.presentLiveWeatherFailure(
          "The weather in " + place.name() + " is of an unknown kind (code " + code + ").");
      return;
    }
    filter.requestWeather(weather.get());
    presenter.presentLiveWeather(
        new LiveWeatherOutputData(
            place.name(),
            WeatherCodes.describe(code).orElseThrow(),
            weather.get().name(),
            ATTRIBUTION));
  }
}
