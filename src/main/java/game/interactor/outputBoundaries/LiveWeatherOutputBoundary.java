package game.interactor.outputBoundaries;

import game.interactor.outputData.LiveWeatherOutputData;

/** Tells the player what live weather is doing. */
public interface LiveWeatherOutputBoundary {

  /**
   * Shows the live weather the game has just taken on.
   *
   * @param data where it is from and what it is
   */
  void presentLiveWeather(LiveWeatherOutputData data);

  /** Shows that live weather is off. */
  void presentLiveWeatherOff();

  /**
   * Shows why live weather could not be found; the game keeps the weather it had.
   *
   * @param reason what went wrong, in words for the player
   */
  void presentLiveWeatherFailure(String reason);
}
