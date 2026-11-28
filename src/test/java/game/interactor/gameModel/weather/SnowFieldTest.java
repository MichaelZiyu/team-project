package game.interactor.gameModel.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SnowFieldTest {

  private static final double FRAME = 1.0 / 60;

  private final RainPerspective perspective = new RainPerspective(800, 600);

  SnowFieldTest() {
    // A single vanishing point on screen, so the number of flakes is the style's.
    perspective.setVanishingOffset(0.12);
    perspective.setOvalWidth(0);
  }

  private SnowField field() {
    return new SnowField(perspective, 800, 600, 5);
  }

  private static void run(SnowField field, double seconds) {
    for (double t = 0; t < seconds; t += FRAME) {
      field.update(FRAME, 300, -200);
    }
  }

  @Test
  void clearSkyHasNoSnow() {
    SnowField field = field();
    run(field, 2);
    assertTrue(field.isEmpty());
  }

  @Test
  void snowBuildsUpGraduallyToItsFullAmount() {
    SnowField field = field();
    field.setStyle(SnowStyle.SNOW);
    run(field, SnowField.RAMP_SECONDS / 2);
    int halfway = field.flakeCount();
    assertTrue(halfway > SnowStyle.SNOW.flakes() / 3 && halfway < SnowStyle.SNOW.flakes() * 2 / 3);
    run(field, SnowField.RAMP_SECONDS);
    assertEquals(SnowStyle.SNOW.flakes(), field.flakeCount());
  }

  @Test
  void flakesLandAndRestAndSnowStopsGently() {
    SnowField field = field();
    field.setStyle(SnowStyle.SNOW);
    run(field, 15);
    assertTrue(field.restingCount() > 0, "some flakes should have landed");
    field.setStyle(SnowStyle.NONE);
    run(field, 1);
    assertTrue(field.flakeCount() > 0, "flakes in the air finish their fall");
    run(field, 30);
    assertTrue(field.isEmpty());
  }

  @Test
  void bothShapesFallAndOnlyFlakesInViewOrItsBorderAreGiven() {
    SnowField field = field();
    field.setStyle(SnowStyle.SNOW);
    run(field, 10);
    Set<SnowflakeShape> shapes = EnumSet.noneOf(SnowflakeShape.class);
    List<double[]> seen = new ArrayList<>();
    field.forEachFlake(
        (x, y, nearness, shape) -> {
          shapes.add(shape);
          seen.add(new double[] {x, y, nearness});
        });
    assertEquals(EnumSet.allOf(SnowflakeShape.class), shapes);
    assertTrue(seen.size() > 50);
    for (double[] flake : seen) {
      double border = FallingArea.MARGIN_PX;
      assertTrue(flake[0] >= -border && flake[0] <= 800 + border);
      assertTrue(flake[1] >= -border && flake[1] <= 600 + border);
      assertTrue(flake[2] >= 0 && flake[2] <= 1);
    }
  }

  @Test
  void flakesFallMoreSlowlyThanRain() {
    // Over the same time, far fewer flakes land, for each one in the air, than raindrops do.
    SnowField snow = field();
    snow.setStyle(SnowStyle.SNOW);
    RainField rain = new RainField(perspective, 800, 600, 5);
    rain.setStyle(RainStyle.THUNDERSTORM);
    int landedFlakes = 0;
    int splashes = 0;
    for (double t = 0; t < 8; t += FRAME) {
      snow.update(FRAME, 300, -200);
      rain.update(FRAME, 300, -200);
      // Landed flakes rest 4 times as long as splashes last; count each about once.
      if (Math.round(t / FRAME) % Math.round(SnowField.REST_SECONDS / FRAME) == 0) {
        landedFlakes += snow.restingCount();
      }
      if (Math.round(t / FRAME) % Math.round(RainField.SPLASH_SECONDS / FRAME) == 0) {
        splashes += rain.splashCount();
      }
    }
    double flakeRate = landedFlakes / (double) snow.flakeCount();
    double dropRate = splashes / (double) rain.dropCount();
    assertTrue(flakeRate * 4 < dropRate, "flakes " + flakeRate + ", drops " + dropRate);
  }

  @Test
  void blizzardsAreThickerFasterAndWindier() {
    SnowStyle snow = SnowStyle.SNOW;
    SnowStyle blizzard = SnowStyle.BLIZZARD;
    assertTrue(blizzard.flakes() > 3 * snow.flakes());
    assertTrue(blizzard.fallSpeedPx() > 2 * snow.fallSpeedPx());
    assertTrue(Math.abs(blizzard.windEastPx()) > 10 * Math.abs(snow.windEastPx()));
  }

  @Test
  void stylesRejectNonsense() {
    assertThrows(IllegalArgumentException.class, () -> new SnowStyle(-1, 100, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> new SnowStyle(10, 0, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> new SnowStyle(10, 100, 0, 0, -1));
    assertThrows(IllegalArgumentException.class, () -> SnowStyle.SNOW.scaled(-1, 1));
    SnowStyle doubled = SnowStyle.SNOW.scaled(2, 0.5);
    assertEquals(2 * SnowStyle.SNOW.flakes(), doubled.flakes());
    assertEquals(SnowStyle.SNOW.fallSpeedPx() / 2, doubled.fallSpeedPx(), 1e-9);
    assertEquals(SnowStyle.SNOW.swayPx(), doubled.swayPx(), 1e-9);
  }

  @Test
  void shapesAreAPlusAndAnX() {
    String plus = picture(SnowflakeShape.PLUS);
    String cross = picture(SnowflakeShape.CROSS);
    assertEquals(".#.###.#.", plus);
    assertEquals("#.#.#.#.#", cross);
  }

  private static String picture(SnowflakeShape shape) {
    StringBuilder picture = new StringBuilder();
    for (int row = 0; row < SnowflakeShape.GRID; row++) {
      for (int column = 0; column < SnowflakeShape.GRID; column++) {
        picture.append(shape.isFilled(column, row) ? '#' : '.');
      }
    }
    return picture.toString();
  }

  @Test
  void aBlizzardStaysEvenlySpreadAcrossTheScreen() {
    SnowField field = field();
    field.setStyle(SnowStyle.BLIZZARD);
    run(field, 20);
    int[] thirds = new int[3];
    field.forEachFlake(
        (x, y, nearness, shape) -> {
          // Only the screen itself, not the border drawn around it.
          if (x >= 0 && x < 800 && y >= 0 && y < 600) {
            thirds[(int) (x / (800 / 3.0))]++;
          }
        });
    int fewest = Math.min(thirds[0], Math.min(thirds[1], thirds[2]));
    int most = Math.max(thirds[0], Math.max(thirds[1], thirds[2]));
    assertTrue(most < 1.5 * fewest, "left, middle, right: " + java.util.Arrays.toString(thirds));
  }
}
