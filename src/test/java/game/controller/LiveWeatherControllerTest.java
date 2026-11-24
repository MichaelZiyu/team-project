package game.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.interactor.inputBoundaries.LiveWeatherInputBoundary;
import game.interactor.inputData.LiveWeatherInputData;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LiveWeatherControllerTest {

  /** Records what the use case is asked to do, and on which thread. */
  private static final class RecordingUseCase implements LiveWeatherInputBoundary {
    final List<String> calls = new CopyOnWriteArrayList<>();
    final List<String> threads = new CopyOnWriteArrayList<>();

    @Override
    public void changeLiveWeather(LiveWeatherInputData input) {
      calls.add(input.source() + (input.city().isEmpty() ? "" : " " + input.city()));
      threads.add(Thread.currentThread().getName());
    }

    @Override
    public void refreshLiveWeather() {
      calls.add("refresh");
    }
  }

  private final RecordingUseCase useCase = new RecordingUseCase();
  private final ScheduledThreadPoolExecutor background = new ScheduledThreadPoolExecutor(1);
  private final LiveWeatherController controller;

  LiveWeatherControllerTest() {
    background.setRemoveOnCancelPolicy(true);
    controller = new LiveWeatherController(useCase, background);
  }

  @AfterEach
  void stop() {
    background.shutdownNow();
  }

  /** Waits until everything handed to the background thread so far has run. */
  private void settle() throws Exception {
    background.submit(() -> {}).get(5, TimeUnit.SECONDS);
  }

  private long scheduledRefreshes() {
    return background.getQueue().stream().filter(task -> task instanceof ScheduledFuture).count();
  }

  @Test
  void choicesReachTheUseCaseInOrderOffTheCallersThread() throws Exception {
    controller.useCity("Toronto");
    controller.useNearby();
    controller.turnOff();
    settle();
    assertEquals(List.of("CITY Toronto", "NEARBY", "OFF"), useCase.calls);
    assertTrue(
        useCase.threads.stream().noneMatch(Thread.currentThread().getName()::equals),
        "looked up off the caller's thread");
  }

  @Test
  void liveWeatherIsLookedUpAgainEveryFifteenMinutes() throws Exception {
    controller.useCity("Toronto");
    settle();
    assertEquals(1, scheduledRefreshes());
    ScheduledFuture<?> refresh = (ScheduledFuture<?>) background.getQueue().peek();
    long minutes = refresh.getDelay(TimeUnit.MINUTES);
    assertTrue(minutes >= 14 && minutes <= 15, "next refresh in " + minutes + " minutes");
  }

  @Test
  void choosingAgainReplacesTheRefreshes() throws Exception {
    controller.useCity("Toronto");
    controller.useNearby();
    settle();
    assertEquals(1, scheduledRefreshes());
  }

  @Test
  void turningOffStopsTheRefreshes() throws Exception {
    controller.useCity("Toronto");
    controller.turnOff();
    settle();
    assertEquals(0, scheduledRefreshes());
  }
}
