package game.interactor.outputData;

/**
 * The live weather the game now shows.
 *
 * @param place where the weather is from, such as "Toronto, Canada"
 * @param conditions the real weather there, such as "Overcast"
 * @param gameWeather the game's weather it became, such as "CLOUDY"
 * @param attribution the credit the weather service's licence asks to be shown wherever its data
 *     is shown
 */
public record LiveWeatherOutputData(
    String place, String conditions, String gameWeather, String attribution) {}
