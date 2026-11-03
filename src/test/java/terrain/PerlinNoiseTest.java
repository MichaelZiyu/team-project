package terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PerlinNoiseTest {

  private static final long SEED = 12345L;

  @Test
  void sameSeedGivesSameValues() {
    PerlinNoise first = new PerlinNoise(SEED);
    PerlinNoise second = new PerlinNoise(SEED);
    for (double x = -20; x < 20; x += 0.37) {
      assertEquals(first.noise(x, x * 0.5), second.noise(x, x * 0.5));
      assertEquals(first.fractal(x, -x, 4), second.fractal(x, -x, 4));
    }
  }

  @Test
  void differentSeedsGiveDifferentValues() {
    PerlinNoise first = new PerlinNoise(SEED);
    PerlinNoise second = new PerlinNoise(SEED + 1);
    int differences = 0;
    for (double x = 0.5; x < 50; x += 1) {
      if (first.noise(x, 0.5) != second.noise(x, 0.5)) {
        differences++;
      }
    }
    assertTrue(differences > 40, "only " + differences + " of 50 samples differed");
  }

  @Test
  void noiseIsZeroAtGridCorners() {
    PerlinNoise noise = new PerlinNoise(SEED);
    for (int x = -5; x <= 5; x++) {
      for (int y = -5; y <= 5; y++) {
        assertEquals(0.0, noise.noise(x, y), 1e-12);
      }
    }
  }

  @Test
  void valuesStayWithinRange() {
    PerlinNoise noise = new PerlinNoise(SEED);
    for (double x = -30; x < 30; x += 0.13) {
      for (double y = -30; y < 30; y += 0.29) {
        double plain = noise.noise(x, y);
        double layered = noise.fractal(x, y, 5);
        assertTrue(plain >= -1 && plain <= 1, "noise out of range: " + plain);
        assertTrue(layered >= -1 && layered <= 1, "fractal out of range: " + layered);
      }
    }
  }

  @Test
  void valuesActuallyVary() {
    PerlinNoise noise = new PerlinNoise(SEED);
    double min = 1;
    double max = -1;
    for (double x = 0.5; x < 40; x += 0.7) {
      for (double y = 0.5; y < 40; y += 0.7) {
        double value = noise.noise(x, y);
        min = Math.min(min, value);
        max = Math.max(max, value);
      }
    }
    assertTrue(min < -0.3 && max > 0.3, "range too narrow: " + min + " to " + max);
  }

  @Test
  void nearbyPointsHaveNearbyValues() {
    PerlinNoise noise = new PerlinNoise(SEED);
    double step = 1e-4;
    // Crosses grid lines, including the origin, to check there are no jumps at cell edges.
    for (double x = -3; x < 3; x += 0.01) {
      double change = Math.abs(noise.noise(x + step, 0.3) - noise.noise(x, 0.3));
      assertTrue(change < 0.01, "jump of " + change + " near x = " + x);
    }
  }

  @Test
  void fractalAtOriginDependsOnSeed() {
    // Plain noise is always 0 at grid corners, but layered noise must not be stuck there.
    int nonZero = 0;
    for (long seed = 0; seed < 20; seed++) {
      if (Math.abs(new PerlinNoise(seed).fractal(0, 0, 4)) > 1e-6) {
        nonZero++;
      }
    }
    assertEquals(20, nonZero);
  }

  @Test
  void fractalRejectsFewerThanOneOctave() {
    PerlinNoise noise = new PerlinNoise(SEED);
    assertThrows(IllegalArgumentException.class, () -> noise.fractal(0, 0, 0));
  }
}
