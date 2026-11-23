package game.interactor.outputData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.interactor.outputData.RenderableData.Layer;
import game.interactor.outputData.RenderableData.LoopingSound;
import game.interactor.outputData.RenderableData.SoundEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RenderableDataTest {

  @Test
  void aPictureOnlyFrameIsSilent() {
    RenderableData frame = new RenderableData(0, 0, List.of());
    assertTrue(frame.loopingSounds().isEmpty());
    assertTrue(frame.soundEvents().isEmpty());
  }

  @Test
  void aFrameCarriesLoopingSoundsAndSoundEvents() {
    RenderableData frame =
        new RenderableData(
            10,
            20,
            List.of(),
            List.of(new LoopingSound("weather/light-rain", 0.6)),
            List.of(new SoundEvent(7, "weather/thunder-close", 1)));
    assertEquals("weather/light-rain", frame.loopingSounds().get(0).name());
    assertEquals(0.6, frame.loopingSounds().get(0).volume(), 1e-9);
    assertEquals(7, frame.soundEvents().get(0).number());
  }

  @Test
  void aFrameDoesNotChangeWhenTheListsItWasMadeFromDo() {
    List<Layer> layers = new ArrayList<>();
    List<LoopingSound> looping = new ArrayList<>(List.of(new LoopingSound("a", 1)));
    List<SoundEvent> events = new ArrayList<>(List.of(new SoundEvent(1, "b", 1)));
    RenderableData frame = new RenderableData(0, 0, layers, looping, events);
    layers.add(Layer.inWorld(List.of()));
    looping.clear();
    events.clear();
    assertTrue(frame.layers().isEmpty());
    assertEquals(1, frame.loopingSounds().size());
    assertEquals(1, frame.soundEvents().size());
    assertThrows(UnsupportedOperationException.class, () -> frame.soundEvents().clear());
  }

  @Test
  void eachLoopingSoundAndEachEventNumberAppearsOnce() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RenderableData(
                0,
                0,
                List.of(),
                List.of(new LoopingSound("rain", 1), new LoopingSound("rain", 0.5)),
                List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RenderableData(
                0,
                0,
                List.of(),
                List.of(),
                List.of(new SoundEvent(3, "thunder", 1), new SoundEvent(3, "bell", 1))));
  }

  @Test
  void soundsNeedANameAndAVolumeFromZeroToOne() {
    assertThrows(IllegalArgumentException.class, () -> new LoopingSound("", 1));
    assertThrows(IllegalArgumentException.class, () -> new LoopingSound(null, 1));
    assertThrows(IllegalArgumentException.class, () -> new LoopingSound("rain", 1.5));
    assertThrows(IllegalArgumentException.class, () -> new SoundEvent(1, "thunder", -0.1));
    assertThrows(IllegalArgumentException.class, () -> new SoundEvent(1, " ", 0.5));
    new LoopingSound("rain", 0);
    new SoundEvent(1, "thunder", 1);
  }
}
