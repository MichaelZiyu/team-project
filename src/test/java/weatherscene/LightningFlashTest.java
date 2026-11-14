package weatherscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LightningFlashTest {

  private static final double FRAME = 1.0 / 60;

  @Test
  void noStrikesWhileSwitchedOff() {
    LightningFlash lightning = new LightningFlash(1);
    for (int i = 0; i < 60 * 30; i++) {
      lightning.update(FRAME);
      assertEquals(0, lightning.brightness());
    }
  }

  @Test
  void strikesComeWithinTheGapRange() {
    LightningFlash lightning = new LightningFlash(1);
    lightning.setActive(true);
    double time = 0;
    double lastStrike = 0;
    int strikes = 0;
    double previous = 0;
    while (time < 120) {
      lightning.update(FRAME);
      time += FRAME;
      double now = lightning.brightness();
      // A strike is the moment the brightness jumps to full; the flicker never reaches it.
      if (now == 1 && previous < 1) {
        double gap = time - lastStrike;
        if (strikes > 0) {
          assertTrue(gap >= LightningFlash.MIN_GAP_SECONDS - FRAME, "gap " + gap);
          assertTrue(gap <= LightningFlash.MAX_GAP_SECONDS + FRAME, "gap " + gap);
        }
        lastStrike = time;
        strikes++;
      }
      previous = now;
    }
    assertTrue(strikes >= 120 / LightningFlash.MAX_GAP_SECONDS - 1, "strikes: " + strikes);
  }

  @Test
  void aStrikeJumpsToFullHoldsThenFlickersThenFades() {
    LightningFlash lightning = new LightningFlash(1);
    lightning.strikeNow();
    assertEquals(1, lightning.brightness(), "full at once, no ramp up");
    lightning.update(LightningFlash.FLASH_HOLD_SECONDS * 0.9);
    assertEquals(1, lightning.brightness(), "held at full");
    lightning.update(LightningFlash.FLASH_HOLD_SECONDS * 0.1 + LightningFlash.FLASH_FADE_SECONDS / 2);
    assertEquals(0.5, lightning.brightness(), 1e-9);
    lightning.update(
        LightningFlash.FLICKER_START_SECONDS
            - LightningFlash.FLASH_HOLD_SECONDS
            - LightningFlash.FLASH_FADE_SECONDS / 2);
    assertEquals(LightningFlash.FLICKER_BRIGHTNESS, lightning.brightness(), 1e-9);
    lightning.update(1);
    assertEquals(0, lightning.brightness());
  }
}
