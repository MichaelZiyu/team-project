package game.interactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.interactor.dataAccess.LiveWeatherDataAccessInterface;
import game.interactor.dataAccess.LiveWeatherDataAccessInterface.Place;
import game.interactor.dataAccess.LiveWeatherException;
import game.interactor.gameModel.scene.Filter;
import game.interactor.gameModel.weather.Weather;
import game.interactor.inputData.LiveWeatherInputData;
import game.interactor.outputBoundaries.LiveWeatherOutputBoundary;
import game.interactor.outputData.LiveWeatherOutputData;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LiveWeatherInteractorTest {

  private static final Place TORONTO = new Place("Toronto, Canada", 43.7, -79.4);
  private static final Place HERE = new Place("Somewhere, Canada", 45, -75);

  /** Stand-in data access: one city, a guessed location, and a weather code to report. */
  private static final class FakeDataAccess implements LiveWeatherDataAccessInterface {
    int code = 3;
    boolean offline;
    final List<Place> weatherAskedFor = new ArrayList<>();

    @Override
    public Place findCity(String city)
        throws LiveWeatherException {
      if (offline) {
        throw new LiveWeatherException("offline");
      }
      if (!city.equals("Toronto")) {
        throw new LiveWeatherException("No city called \"" + city + "\" was found.");
      }
      return TORONTO;
    }

    @Override
    public Place guessLocation() throws LiveWeatherException {
      if (offline) {
        throw new LiveWeatherException("offline");
      }
      return HERE;
    }

    @Override
    public int currentWeatherCode(Place place)
        throws LiveWeatherException {
      if (offline) {
        throw new LiveWeatherException("offline");
      }
      weatherAskedFor.add(place);
      return code;
    }
  }

  /** Records what the player is told. */
  private static final class RecordingPresenter implements LiveWeatherOutputBoundary {
    final List<String> told = new ArrayList<>();
    LiveWeatherOutputData last;

    @Override
    public void presentLiveWeather(LiveWeatherOutputData data) {
      last = data;
      told.add("weather");
    }

    @Override
    public void presentLiveWeatherOff() {
      told.add("off");
    }

    @Override
    public void presentLiveWeatherFailure(String reason) {
      told.add("failure: " + reason);
    }
  }

  private final FakeDataAccess dataAccess = new FakeDataAccess();
  private final RecordingPresenter presenter = new RecordingPresenter();
  private final Filter filter = new Filter();
  private final LiveWeatherInteractor interactor =
      new LiveWeatherInteractor(dataAccess, presenter, filter);

  /** The filter's weather after the next game tick, when asked-for weather takes effect. */
  private Weather weatherAfterTick() {
    filter.tick(null);
    return filter.getWeather();
  }

  @Test
  void citysWeatherBecomesTheGamesWeather() {
    interactor.changeLiveWeather(LiveWeatherInputData.city("Toronto"));
    assertEquals(Weather.CLOUDY, weatherAfterTick());
    assertEquals(
        new LiveWeatherOutputData(
            "Toronto, Canada", "Overcast", "CLOUDY", LiveWeatherInteractor.ATTRIBUTION),
        presenter.last);
  }

  @Test
  void theWeatherWhereThePlayerSeemsToBeBecomesTheGamesWeather() {
    dataAccess.code = 75;
    interactor.changeLiveWeather(LiveWeatherInputData.nearby());
    assertEquals(Weather.BLIZZARD, weatherAfterTick());
    assertEquals(List.of(HERE), dataAccess.weatherAskedFor);
    assertEquals("Somewhere, Canada", presenter.last.place());
  }

  @Test
  void refreshingLooksTheWeatherUpAgainAtTheSamePlace() {
    interactor.changeLiveWeather(LiveWeatherInputData.city("Toronto"));
    dataAccess.code = 95;
    interactor.refreshLiveWeather();
    assertEquals(Weather.THUNDERSTORM, weatherAfterTick());
    assertEquals(List.of(TORONTO, TORONTO), dataAccess.weatherAskedFor);
  }

  @Test
  void turningItOffStopsRefreshingAndKeepsTheWeather() {
    interactor.changeLiveWeather(LiveWeatherInputData.city("Toronto"));
    interactor.changeLiveWeather(LiveWeatherInputData.off());
    dataAccess.code = 61;
    interactor.refreshLiveWeather();
    assertEquals(Weather.CLOUDY, weatherAfterTick());
    assertEquals(List.of("weather", "off"), presenter.told);
  }

  @Test
  void anUnknownCityIsExplainedAndTheWeatherKept() {
    interactor.changeLiveWeather(LiveWeatherInputData.city("Qzxqzx"));
    assertEquals(Weather.CLEAR, weatherAfterTick());
    assertEquals(List.of("failure: No city called \"Qzxqzx\" was found."), presenter.told);
    interactor.refreshLiveWeather();
    assertTrue(dataAccess.weatherAskedFor.isEmpty(), "nothing to refresh without a place");
  }

  @Test
  void blankCityIsExplained() {
    interactor.changeLiveWeather(LiveWeatherInputData.city("   "));
    assertEquals(List.of("failure: Type the name of a city."), presenter.told);
  }

  @Test
  void beingOfflineIsExplainedAndTheWeatherKept() {
    interactor.changeLiveWeather(LiveWeatherInputData.city("Toronto"));
    dataAccess.offline = true;
    interactor.refreshLiveWeather();
    assertEquals(Weather.CLOUDY, weatherAfterTick());
    assertEquals(List.of("weather", "failure: offline"), presenter.told);
  }

  @Test
  void anUnknownWeatherCodeIsExplainedAndTheWeatherKept() {
    dataAccess.code = 4;
    interactor.changeLiveWeather(LiveWeatherInputData.city("Toronto"));
    assertEquals(Weather.CLEAR, weatherAfterTick());
    assertTrue(presenter.told.get(0).contains("unknown kind (code 4)"), presenter.told.get(0));
  }
}
