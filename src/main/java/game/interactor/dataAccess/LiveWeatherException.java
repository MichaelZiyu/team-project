package game.interactor.dataAccess;

/** Live weather, or the place it is for, could not be found. */
public class LiveWeatherException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param reason what went wrong, in words for the player
   */
  public LiveWeatherException(String reason) {
    super(reason);
  }

  /**
   * Creates the exception from the problem that caused it.
   *
   * @param reason what went wrong, in words for the player
   * @param cause the problem underneath, such as a lost connection
   */
  public LiveWeatherException(String reason, Throwable cause) {
    super(reason, cause);
  }
}
