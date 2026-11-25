package weatherscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FogFieldTest {

  private static final int AREA_PX = 4000;
  private static final int STEP_PX = 40;

  @Test
  void thicknessIsBetweenZeroAndOne() {
    FogField fog = new FogField(1);
    for (int x = -AREA_PX; x < AREA_PX; x += STEP_PX) {
      for (int y = -AREA_PX; y < AREA_PX; y += STEP_PX) {
        double thickness = fog.thickness(x, y);
        assertTrue(thickness >= 0 && thickness <= 1, "thickness " + thickness);
      }
    }
  }

  @Test
  void fogHasDenseBanksAndThinGapsNotAnEvenGrey() {
    FogField fog = new FogField(2);
    int dense = 0;
    int thin = 0;
    int samples = 0;
    for (int x = 0; x < AREA_PX; x += STEP_PX) {
      for (int y = 0; y < AREA_PX; y += STEP_PX) {
        double thickness = fog.thickness(x, y);
        samples++;
        if (thickness > 0.9) {
          dense++;
        } else if (thickness < 0.1) {
          thin++;
        }
      }
    }
    assertTrue(dense > samples / 20, "dense banks: " + dense + " of " + samples);
    assertTrue(thin > samples / 20, "thin gaps: " + thin + " of " + samples);
  }

  @Test
  void thicknessChangesSmoothlyFromPlaceToPlace() {
    FogField fog = new FogField(3);
    for (int x = 0; x < AREA_PX; x += STEP_PX) {
      for (int y = 0; y < AREA_PX; y += STEP_PX) {
        double here = fog.thickness(x, y);
        double right = fog.thickness(x + WeatherLayer.FOG_CELL_PX, y);
        double up = fog.thickness(x, y + WeatherLayer.FOG_CELL_PX);
        assertTrue(Math.abs(right - here) < 0.25, "jump across at " + x + ", " + y);
        assertTrue(Math.abs(up - here) < 0.45, "jump up at " + x + ", " + y);
      }
    }
  }

  @Test
  void theSameSeedGivesTheSameFog() {
    FogField first = new FogField(4);
    FogField second = new FogField(4);
    first.update(3);
    second.update(3);
    assertEquals(first.thickness(123, -456), second.thickness(123, -456));
  }

  @Test
  void fogDriftsOverTime() {
    FogField fog = new FogField(5);
    double before = sumOfThickness(fog);
    fog.update(20);
    assertNotEquals(before, sumOfThickness(fog));
  }

  @Test
  void fogStandsStillAtZeroSpeed() {
    FogField fog = new FogField(6);
    double before = sumOfThickness(fog);
    fog.setSpeed(0);
    fog.update(20);
    assertEquals(before, sumOfThickness(fog));
  }

  @Test
  void aNegativeSpeedIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new FogField(7).setSpeed(-1));
  }

  private static double sumOfThickness(FogField fog) {
    double sum = 0;
    for (int x = 0; x < 1000; x += 50) {
      for (int y = 0; y < 1000; y += 50) {
        sum += fog.thickness(x, y);
      }
    }
    return sum;
  }
}
