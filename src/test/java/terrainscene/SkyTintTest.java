package terrainscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.gameModel.Renderable;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
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

  @Test
  void layerCoversTheWholeScreenAtNightAndIsEmptyAtNoon() {
    SkyTintLayer layer = new SkyTintLayer(800, 600);
    layer.update(1234.7, -567.2, 0);
    List<Renderable> contents = contents(layer);
    assertEquals(1, contents.size());
    Renderable wash = contents.get(0);
    // The game camera's placement rule, for a player at whole pixel (1234, -567).
    int screenX = wash.getX() - 1234 + 800 / 2 - wash.getWidth() / 2;
    int screenY = 600 / 2 - (wash.getY() - (-567)) - wash.getHeight();
    int overhang = SkyTintLayer.OVERHANG_PX;
    assertEquals(-overhang, screenX);
    assertEquals(-overhang, screenY);
    assertEquals(800 + 2 * overhang, wash.getWidth());
    assertEquals(600 + 2 * overhang, wash.getHeight());
    layer.update(0, 0, 0.5);
    assertTrue(contents(layer).isEmpty());
    assertNull(layer.currentColour());
  }

  @Test
  void layerReusesItsImageUntilTheColourVisiblyChanges() {
    SkyTintLayer layer = new SkyTintLayer(800, 600);
    layer.update(0, 0, 0.1);
    Renderable first = contents(layer).get(0);
    layer.update(50, 50, 0.1);
    assertSame(first, contents(layer).get(0), "night stays the same colour");
    layer.update(50, 50, 0.24);
    assertNotNull(layer.currentColour());
    assertTrue(first != contents(layer).get(0), "dawn is a different colour");
  }

  private static List<Renderable> contents(SkyTintLayer layer) {
    List<Renderable> contents = new ArrayList<>();
    layer.forEach(contents::add);
    return contents;
  }
}
