package game.interactor.inputBoundaries;

import game.interactor.inputData.LiveWeatherInputData;

/**
 * Matches the game's weather to the real weather somewhere, if the player wants that.
 *
 * <p>Both methods may wait on the internet, so call them off the Swing thread and the game loop.
 */
public interface LiveWeatherInputBoundary {

  /**
   * Changes where live weather comes from, or turns it off, and, if it is on, brings the game's
   * weather up to date with it straight away.
   *
   * @param input the player's choice
   */
  void changeLiveWeather(LiveWeatherInputData input);

  /** Brings the game's weather up to date with the live weather again, if it is on. */
  void refreshLiveWeather();
}
