package game.interactor.gameModel.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.interactor.outputData.RenderableData.LoopingSound;
import java.util.List;
import org.junit.jupiter.api.Test;

class WeatherSoundTest {

  private static final double FRAME = 1.0 / 60;

  private final WeatherSound sound = new WeatherSound();

  private void run(double seconds) {
    for (double t = 0; t < seconds - 1e-9; t += FRAME) {
      sound.update(FRAME);
    }
  }

  @Test
  void clearWeatherIsSilent() {
    run(1);
    assertTrue(sound.loops().isEmpty());
  }

  @Test
  void rainFadesTheLightRainIn() {
    sound.setWeather(Weather.RAIN);
    run(WeatherSound.FADE_SECONDS / 2);
    assertEquals(1, sound.loops().size());
    assertEquals(WeatherSound.LIGHT_RAIN, sound.loops().get(0).name());
    assertEquals(0.5, sound.loops().get(0).volume(), 0.02);
    run(WeatherSound.FADE_SECONDS);
    assertEquals(List.of(new LoopingSound(WeatherSound.LIGHT_RAIN, 1)), sound.loops());
  }

  @Test
  void aThunderstormBlendsFromLightRainToHeavyRain() {
    sound.setWeather(Weather.RAIN);
    run(WeatherSound.FADE_SECONDS);
    sound.setWeather(Weather.THUNDERSTORM);
    run(WeatherSound.FADE_SECONDS / 2);
    assertEquals(0.5, sound.lightRainLevel(), 0.02);
    assertEquals(0.5, sound.heavyRainLevel(), 0.02);
    assertEquals(2, sound.loops().size(), "both heard while blending");
    run(WeatherSound.FADE_SECONDS);
    assertEquals(List.of(new LoopingSound(WeatherSound.HEAVY_RAIN, 1)), sound.loops());
  }

  @Test
  void dryWeatherFadesTheRainOutUntilItIsGone() {
    for (Weather dry : new Weather[] {Weather.CLEAR, Weather.SNOW, Weather.BLIZZARD}) {
      sound.setWeather(Weather.THUNDERSTORM);
      run(WeatherSound.FADE_SECONDS);
      sound.setWeather(dry);
      run(WeatherSound.FADE_SECONDS / 2);
      assertTrue(sound.heavyRainLevel() > 0.4 && sound.heavyRainLevel() < 0.6, dry + " fades");
      run(WeatherSound.FADE_SECONDS);
      assertTrue(sound.loops().isEmpty(), dry + " is silent");
    }
  }
}
