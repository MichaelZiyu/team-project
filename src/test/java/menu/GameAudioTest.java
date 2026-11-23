package menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.gameOutput.GameAudioBoundary.Loop;
import game.gameOutput.GameAudioBoundary.OneShot;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameAudioTest {

  @Test
  void fullVolumeIsZeroDecibelsAndQuieterIsLess() {
    assertEquals(0, GameAudio.decibels(1), 1e-6);
    assertEquals(-6.02, GameAudio.decibels(0.5), 0.01);
    assertEquals(-20, GameAudio.decibels(0.1), 1e-4);
    assertTrue(GameAudio.decibels(0) <= -60, "silent is as quiet as it goes");
    assertTrue(GameAudio.decibels(0.0000001) >= -60, "never below the floor");
  }

  @Test
  void aSeamlessLoopRunsFromItsEndStraightIntoItsStart() {
    // A mono ramp: each sample one more than the last, so a jump anywhere shows.
    short[] ramp = new short[1000];
    for (int i = 0; i < ramp.length; i++) {
      ramp[i] = (short) i;
    }
    short[] loop = GameAudio.seamlessLoop(ramp, 1, 100);
    assertEquals(900, loop.length);
    assertEquals(899, loop[loop.length - 1]);
    assertEquals(900, loop[0], 2, "the start begins with what followed the end");
    assertEquals(100, loop[100], "after the blend, the sound's own start");
    for (int i = 1; i < loop.length; i++) {
      assertTrue(Math.abs(loop[i] - loop[i - 1]) < 20, "jump at " + i);
    }
    assertThrows(IllegalArgumentException.class, () -> GameAudio.seamlessLoop(ramp, 1, 600));
  }

  @Test
  void missingSoundsAreSimplyNotHeard() {
    GameAudio audio = new GameAudio();
    audio.update(
        List.of(new Loop("no/such-loop", 1)), List.of(new OneShot("no/such-sound", 1)));
    audio.update(List.of(), List.of());
    audio.close();
  }
}
