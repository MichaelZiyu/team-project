package game.interactor.gameModel.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.interactor.outputData.RenderableData;
import game.interactor.outputData.RenderableData.SoundEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ThunderSoundTest {

  private static final double FRAME = 1.0 / 60;

  private static void run(ThunderSound thunder, double seconds) {
    for (double t = 0; t < seconds - 1e-9; t += FRAME) {
      thunder.update(FRAME);
    }
  }

  @Test
  void strongerFlashesThunderSooner() {
    assertEquals(0, ThunderSound.delayFor(1), 1e-9);
    assertEquals(
        ThunderSound.MAX_DELAY_SECONDS, ThunderSound.delayFor(LightningFlash.MIN_INTENSITY), 1e-9);
    double middle = (1 + LightningFlash.MIN_INTENSITY) / 2;
    assertEquals(ThunderSound.MAX_DELAY_SECONDS / 2, ThunderSound.delayFor(middle), 1e-9);
  }

  @Test
  void thunderIsHeardOnceItsDelayIsOver() {
    ThunderSound thunder = new ThunderSound(3);
    thunder.strike((1 + LightningFlash.MIN_INTENSITY) / 2);
    run(thunder, 1.9);
    assertTrue(thunder.events().isEmpty(), "still on its way");
    assertEquals(1, thunder.pendingCount());
    run(thunder, 0.2);
    assertEquals(1, thunder.events().size());
    assertEquals(0, thunder.pendingCount());
    assertTrue(ThunderSound.SOUNDS.contains(thunder.events().get(0).name()));
  }

  @Test
  void aClapStaysInTheFramesForAMomentThenGoes() {
    ThunderSound thunder = new ThunderSound(3);
    thunder.strike(1);
    thunder.update(FRAME);
    SoundEvent clap = thunder.events().get(0);
    run(thunder, RenderableData.SOUND_EVENT_SECONDS - 2 * FRAME);
    assertEquals(List.of(clap), thunder.events(), "the same event, number and all");
    run(thunder, 3 * FRAME);
    assertTrue(thunder.events().isEmpty());
  }

  @Test
  void everyClapHasItsOwnNumberAndBothSoundsAreUsed() {
    ThunderSound thunder = new ThunderSound(3);
    Set<Long> numbers = new HashSet<>();
    Set<String> sounds = new HashSet<>();
    for (int i = 0; i < 40; i++) {
      thunder.strike(1);
      thunder.update(FRAME);
      for (SoundEvent event : thunder.events()) {
        numbers.add(event.number());
        sounds.add(event.name());
      }
      run(thunder, 0.5);
    }
    assertEquals(40, numbers.size());
    assertEquals(Set.copyOf(ThunderSound.SOUNDS), sounds);
  }
}
