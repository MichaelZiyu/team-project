package game.controller;

import game.interactor.inputBoundaries.LiveWeatherInputBoundary;
import game.interactor.inputData.LiveWeatherInputData;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Takes the player's live weather choices from the menu to the live weather use case.
 *
 * <p>Looking weather up waits on the internet, so it happens on a thread of the controller's
 * own, never the Swing thread or the game loop; each method returns at once. While live weather
 * is on, it is looked up again every {@link #REFRESH_MINUTES} minutes, as often as the weather
 * service updates it.
 */
public final class LiveWeatherController {

  /** How often live weather is looked up again, in minutes. */
  static final long REFRESH_MINUTES = 15;

  private final LiveWeatherInputBoundary liveWeather;
  private final ScheduledExecutorService background;

  /** The repeated look-ups while live weather is on; null while it is off. */
  private ScheduledFuture<?> refreshing;

  /**
   * Creates the controller.
   *
   * @param liveWeather the live weather use case
   */
  public LiveWeatherController(LiveWeatherInputBoundary liveWeather) {
    this(liveWeather, Executors.newSingleThreadScheduledExecutor(LiveWeatherController::daemon));
  }

  /**
   * Creates the controller, running the use case on a given thread; used by tests.
   *
   * @param liveWeather the live weather use case
   * @param background runs the look-ups, one at a time
   */
  LiveWeatherController(LiveWeatherInputBoundary liveWeather, ScheduledExecutorService background) {
    this.liveWeather = liveWeather;
    this.background = background;
  }

  /** Turns live weather off; the game keeps the weather it has. */
  public synchronized void turnOff() {
    stopRefreshing();
    background.execute(() -> liveWeather.changeLiveWeather(LiveWeatherInputData.off()));
  }

  /**
   * Makes the game's weather follow a city's.
   *
   * @param city the city's name, as the player typed it
   */
  public synchronized void useCity(String city) {
    start(LiveWeatherInputData.city(city));
  }

  /** Makes the game's weather follow the weather where the player seems to be. */
  public synchronized void useNearby() {
    start(LiveWeatherInputData.nearby());
  }

  /** Stops live weather for good, when the game closes. */
  public synchronized void shutdown() {
    stopRefreshing();
    background.shutdownNow();
  }

  private void start(LiveWeatherInputData choice) {
    stopRefreshing();
    background.execute(() -> liveWeather.changeLiveWeather(choice));
    refreshing =
        background.scheduleAtFixedRate(
            liveWeather::refreshLiveWeather, REFRESH_MINUTES, REFRESH_MINUTES, TimeUnit.MINUTES);
  }

  private void stopRefreshing() {
    if (refreshing != null) {
      refreshing.cancel(false);
      refreshing = null;
    }
  }

  /** A thread that does not keep the game running once everything else has closed. */
  private static Thread daemon(Runnable work) {
    Thread thread = new Thread(work, "live-weather");
    thread.setDaemon(true);
    return thread;
  }
}
