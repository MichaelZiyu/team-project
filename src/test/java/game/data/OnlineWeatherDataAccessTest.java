package game.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.interactor.dataAccess.LiveWeatherDataAccessInterface.Place;
import game.interactor.dataAccess.LiveWeatherException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;

/**
 * Tests reading the web services' answers. No test goes on the internet: answers saved from the
 * real services are handed back by a stand-in connection.
 */
class OnlineWeatherDataAccessTest {

  private static final Place TORONTO = new Place("Toronto, Canada", 43.70643, -79.39864);

  /** The URLs asked for, in order. */
  private final List<HttpUrl> asked = new ArrayList<>();

  private static String saved(String name) throws IOException {
    try (InputStream file =
        OnlineWeatherDataAccessTest.class.getResourceAsStream("/liveweather/" + name)) {
      return new String(file.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Data access whose every request is answered with a saved answer and status code. */
  private OnlineWeatherDataAccess answering(String answer, int status) {
    OkHttpClient client =
        new OkHttpClient.Builder()
            .addInterceptor(
                chain -> {
                  asked.add(chain.request().url());
                  return new Response.Builder()
                      .request(chain.request())
                      .protocol(Protocol.HTTP_1_1)
                      .code(status)
                      .message("stand-in")
                      .body(ResponseBody.create(answer, MediaType.get("application/json")))
                      .build();
                })
            .build();
    return new OnlineWeatherDataAccess(client);
  }

  /** Data access whose every request fails as if offline. */
  private static OnlineWeatherDataAccess offline() {
    OkHttpClient client =
        new OkHttpClient.Builder()
            .addInterceptor(
                chain -> {
                  throw new IOException("offline");
                })
            .build();
    return new OnlineWeatherDataAccess(client);
  }

  @Test
  void readsTheCurrentWeatherCode() throws Exception {
    assertEquals(3, answering(saved("forecast-toronto.json"), 200).currentWeatherCode(TORONTO));
  }

  @Test
  void asksForOnlyTheCurrentWeatherCodeAtThePlace() throws Exception {
    answering(saved("forecast-toronto.json"), 200).currentWeatherCode(TORONTO);
    HttpUrl url = asked.get(0);
    assertEquals("api.open-meteo.com", url.host());
    assertEquals("43.70643", url.queryParameter("latitude"));
    assertEquals("-79.39864", url.queryParameter("longitude"));
    assertEquals("weather_code", url.queryParameter("current"));
  }

  @Test
  void passesOnTheWeatherServicesOwnReasonForAnError() throws Exception {
    LiveWeatherException error =
        assertThrows(
            LiveWeatherException.class,
            () -> answering(saved("forecast-error.json"), 400).currentWeatherCode(TORONTO));
    assertTrue(error.getMessage().contains("Latitude must be in range"), error.getMessage());
  }

  @Test
  void findsCityNamedWithItsCountry() throws Exception {
    Place place = answering(saved("geocoding-toronto.json"), 200).findCity("Toronto");
    assertEquals(new Place("Toronto, Canada", 43.70643, -79.39864), place);
    assertEquals("Toronto", asked.get(0).queryParameter("name"));
    assertEquals("1", asked.get(0).queryParameter("count"));
  }

  @Test
  void saysWhenNoCityMatches() throws Exception {
    LiveWeatherException error =
        assertThrows(
            LiveWeatherException.class,
            () -> answering(saved("geocoding-none.json"), 200).findCity("Qzxqzx"));
    assertEquals("No city called \"Qzxqzx\" was found.", error.getMessage());
  }

  @Test
  void guessesWhereThePlayerIs() throws Exception {
    Place place = answering(saved("locate-success.json"), 200).guessLocation();
    assertEquals(new Place("Toronto, Canada", 43.706428, -79.3986419), place);
    assertEquals("ipwho.is", asked.get(0).host());
  }

  @Test
  void saysWhenWhereThePlayerIsCannotBeGuessed() throws Exception {
    LiveWeatherException error =
        assertThrows(
            LiveWeatherException.class,
            () -> answering(saved("locate-failure.json"), 200).guessLocation());
    assertEquals("Could not work out where you are: Reserved range.", error.getMessage());
  }

  @Test
  void saysWhenThereHaveBeenTooManyRequests() {
    LiveWeatherException error =
        assertThrows(
            LiveWeatherException.class,
            () -> answering("{}", 429).currentWeatherCode(TORONTO));
    assertTrue(error.getMessage().contains("too many requests"), error.getMessage());
  }

  @Test
  void saysWhenTheInternetCannotBeReached() {
    LiveWeatherException error =
        assertThrows(LiveWeatherException.class, () -> offline().currentWeatherCode(TORONTO));
    assertTrue(error.getMessage().contains("internet connection"), error.getMessage());
  }

  @Test
  void saysWhenAnAnswerCannotBeRead() {
    LiveWeatherException error =
        assertThrows(
            LiveWeatherException.class,
            () -> answering("<html>not json</html>", 200).currentWeatherCode(TORONTO));
    assertEquals("The weather service's answer could not be read.", error.getMessage());
  }
}
