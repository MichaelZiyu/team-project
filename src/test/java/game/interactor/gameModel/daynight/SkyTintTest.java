package game.interactor.gameModel.daynight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import org.junit.jupiter.api.Test;

class SkyTintTest {

  @Test
  void middayHasNoWashAndNightIsDimBlue() {
    assertEquals(0, SkyTint.at(0.5).getAlpha());
    assertEquals(SkyTint.NIGHT, SkyTint.at(0));
    assertEquals(SkyTint.NIGHT, SkyTint.at(0.9));
    Color night = SkyTint.at(0.05);
    assertTrue(night.getBlue() > night.getRed(), "night is blue");
  }

  @Test
  void dawnAndDuskGlowOrange() {
    assertEquals(SkyTint.DAWN, SkyTint.at(0.25));
    assertEquals(SkyTint.DUSK, SkyTint.at(0.75));
    assertTrue(SkyTint.DUSK.getRed() > SkyTint.DUSK.getBlue());
  }

  @Test
  void washChangesGraduallyThroughTheDay() {
    Color previous = SkyTint.at(0);
    for (double time = 0.001; time < 1; time += 0.001) {
      Color now = SkyTint.at(time);
      assertTrue(Math.abs(now.getAlpha() - previous.getAlpha()) <= 3, "jump at " + time);
      previous = now;
    }
  }
}
