package game.data;

import game.interactor.dataAccess.LiveWeatherDataAccessInterface;
import game.interactor.dataAccess.LiveWeatherException;
import java.io.IOException;
import java.time.Duration;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Finds places and their weather on the internet, with no keys needed.
 *
 * <ul>
 *   <li>Weather: Open-Meteo's forecast (https://open-meteo.com/), whose current weather code is
 *       updated every 15 minutes. Free for non-commercial use, under CC BY 4.0, so its data must
 *       be credited where it is shown.
 *   <li>Cities: Open-Meteo's geocoding, which finds a city by name.
 *   <li>Where the player is: ipwho.is, which guesses a nearby city from the internet address the
 *       request comes from. Free up to 1,000 requests a day.
 * </ul>
 */
public final class OnlineWeatherDataAccess implements LiveWeatherDataAccessInterface {

  /** Open-Meteo's weather forecast. */
  static final HttpUrl FORECAST = HttpUrl.get("https://api.open-meteo.com/v1/forecast");

  /** Open-Meteo's city search. */
  static final HttpUrl GEOCODING = HttpUrl.get("https://geocoding-api.open-meteo.com/v1/search");

  /** ipwho.is, which locates the internet address a request comes from. */
  static final HttpUrl LOCATE = HttpUrl.get("https://ipwho.is/");

  /** How long to wait for an answer before giving up. */
  static final Duration TIMEOUT = Duration.ofSeconds(10);

  /** Tells the services what is asking, without saying anything about the player. */
  private static final String USER_AGENT = "csc207-team-project-game";

  private static final int TOO_MANY_REQUESTS = 429;

  private final OkHttpClient client;

  /** Creates the data access with its own connection. */
  public OnlineWeatherDataAccess() {
    this(new OkHttpClient.Builder().callTimeout(TIMEOUT).build());
  }

  /**
   * Creates the data access using a given connection.
   *
   * @param client makes the requests
   */
  public OnlineWeatherDataAccess(OkHttpClient client) {
    this.client = client;
  }

  @Override
  public Place findCity(String city) throws LiveWeatherException {
    HttpUrl url =
        GEOCODING
            .newBuilder()
            .addQueryParameter("name", city)
            .addQueryParameter("count", "1")
            .addQueryParameter("language", "en")
            .addQueryParameter("format", "json")
            .build();
    return parseCity(get(url), city);
  }

  @Override
  public Place guessLocation() throws LiveWeatherException {
    return parseLocation(get(LOCATE));
  }

  @Override
  public int currentWeatherCode(Place place) throws LiveWeatherException {
    HttpUrl url =
        FORECAST
            .newBuilder()
            .addQueryParameter("latitude", Double.toString(place.latitude()))
            .addQueryParameter("longitude", Double.toString(place.longitude()))
            .addQueryParameter("current", "weather_code")
            .build();
    return parseWeatherCode(get(url));
  }

  /**
   * Reads the best match from Open-Meteo's city search.
   *
   * @param json the search's answer
   * @param city the name searched for
   * @return the city, named with its country
   * @throws LiveWeatherException if nothing matched, or the answer cannot be read
   */
  static Place parseCity(String json, String city) throws LiveWeatherException {
    try {
      JSONArray results = new JSONObject(json).optJSONArray("results");
      if (results == null || results.isEmpty()) {
        throw new LiveWeatherException("No city called \"" + city + "\" was found.");
      }
      JSONObject match = results.getJSONObject(0);
      return new Place(
          nameWithCountry(match.getString("name"), match.optString("country")),
          match.getDouble("latitude"),
          match.getDouble("longitude"));
    } catch (JSONException e) {
      throw unreadable(e);
    }
  }

  /**
   * Reads where ipwho.is guessed the player is.
   *
   * @param json its answer
   * @return the place, named by its city and country
   * @throws LiveWeatherException if it could not guess, or the answer cannot be read
   */
  static Place parseLocation(String json) throws LiveWeatherException {
    try {
      JSONObject answer = new JSONObject(json);
      if (!answer.optBoolean("success", false)) {
        throw new LiveWeatherException(
            "Could not work out where you are: "
                + answer.optString("message", "no reason given")
                + ".");
      }
      return new Place(
          nameWithCountry(answer.optString("city", "Near you"), answer.optString("country")),
          answer.getDouble("latitude"),
          answer.getDouble("longitude"));
    } catch (JSONException e) {
      throw unreadable(e);
    }
  }

  /**
   * Reads the current weather code from Open-Meteo's forecast.
   *
   * @param json the forecast's answer
   * @return the WMO weather code
   * @throws LiveWeatherException if the answer is an error or cannot be read
   */
  static int parseWeatherCode(String json) throws LiveWeatherException {
    try {
      JSONObject answer = new JSONObject(json);
      if (answer.optBoolean("error", false)) {
        throw new LiveWeatherException(
            "The weather service said: " + answer.optString("reason", "no reason given"));
      }
      return answer.getJSONObject("current").getInt("weather_code");
    } catch (JSONException e) {
      throw unreadable(e);
    }
  }

  /** Asks for a URL and returns the answer, or says in words why there is none. */
  private String get(HttpUrl url) throws LiveWeatherException {
    Request request = new Request.Builder().url(url).header("User-Agent", USER_AGENT).build();
    try (Response response = client.newCall(request).execute()) {
      ResponseBody body = response.body();
      String text = body == null ? "" : body.string();
      if (response.code() == TOO_MANY_REQUESTS) {
        throw new LiveWeatherException(
            "The weather service has had too many requests; try again later.");
      }
      // Open-Meteo explains its errors in the answer, which the readers above pass on.
      if (!response.isSuccessful() && !text.contains("\"reason\"")) {
        throw new LiveWeatherException(
            "The weather service could not answer (error " + response.code() + ").");
      }
      return text;
    } catch (IOException e) {
      throw new LiveWeatherException(
          "Could not reach the weather service; check the internet connection.", e);
    }
  }

  private static String nameWithCountry(String name, String country) {
    return country == null || country.isBlank() ? name : name + ", " + country;
  }

  private static LiveWeatherException unreadable(JSONException e) {
    return new LiveWeatherException("The weather service's answer could not be read.", e);
  }
}
