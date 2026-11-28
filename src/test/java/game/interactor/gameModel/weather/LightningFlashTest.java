package game.interactor.gameModel.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class LightningFlashTest {

  private static final double FRAME = 1.0 / 60;

  /** Plans many flashes from one lightning and returns their strokes. */
  private static List<List<LightningFlash.Stroke>> planMany(int flashes) {
    LightningFlash lightning = new LightningFlash(21);
    List<List<LightningFlash.Stroke>> plans = new ArrayList<>();
    for (int i = 0; i < flashes; i++) {
      plans.add(lightning.upcomingStrokes());
      lightning.strikeNow();
    }
    return plans;
  }

  @Test
  void noFlashesWhileSwitchedOff() {
    LightningFlash lightning = new LightningFlash(1);
    for (int i = 0; i < 60 * 30; i++) {
      lightning.update(FRAME);
      assertEquals(0, lightning.brightness());
    }
  }

  @Test
  void flashesComeAtRandomButNeverTooClose() {
    LightningFlash lightning = new LightningFlash(1);
    lightning.setActive(true);
    List<Double> starts = new ArrayList<>();
    double time = 0;
    double previousAngle = lightning.strikeAngle();
    while (time < 2000) {
      lightning.update(FRAME);
      time += FRAME;
      if (lightning.strikeAngle() != previousAngle) {
        starts.add(time);
        previousAngle = lightning.strikeAngle();
      }
    }
    double total = 0;
    for (int i = 1; i < starts.size(); i++) {
      double gap = starts.get(i) - starts.get(i - 1);
      assertTrue(gap >= LightningFlash.MIN_GAP_SECONDS - FRAME, "gap " + gap);
      total += gap;
    }
    double mean = total / (starts.size() - 1);
    assertTrue(
        Math.abs(mean - LightningFlash.MEAN_GAP_SECONDS) < 0.2 * LightningFlash.MEAN_GAP_SECONDS,
        "average wait " + mean);
  }

  @Test
  void strokeCountsFollowTheirChances() {
    int[] counts = new int[6];
    List<List<LightningFlash.Stroke>> plans = planMany(5000);
    for (List<LightningFlash.Stroke> strokes : plans) {
      counts[strokes.size()]++;
    }
    for (int strokes = 1; strokes <= 5; strokes++) {
      double share = counts[strokes] / 5000.0;
      double expected = LightningFlash.STROKE_COUNT_CHANCES[strokes - 1];
      assertTrue(Math.abs(share - expected) < 0.03, strokes + " strokes: " + share);
    }
  }

  @Test
  void strokeGapsAreMostlyAroundTheMedianWithTheOddLongOne() {
    List<Double> gaps = new ArrayList<>();
    for (List<LightningFlash.Stroke> strokes : planMany(3000)) {
      for (int i = 1; i < strokes.size(); i++) {
        double gap = strokes.get(i).start() - strokes.get(i - 1).start();
        assertTrue(gap >= LightningFlash.MIN_STROKE_GAP_SECONDS - 1e-12, "gap " + gap);
        assertTrue(gap <= LightningFlash.MAX_STROKE_GAP_SECONDS + 1e-12, "gap " + gap);
        gaps.add(gap);
      }
    }
    Collections.sort(gaps);
    double median = gaps.get(gaps.size() / 2);
    double expected = LightningFlash.MEDIAN_STROKE_GAP_SECONDS;
    assertTrue(Math.abs(median - expected) < 0.1 * expected, "median gap " + median);
    double longest = gaps.get(gaps.size() - 1);
    assertTrue(longest > 2.5 * expected, "some gaps are much longer: " + longest);
  }

  @Test
  void laterStrokesAreWeakerThanTheFirstAndAllFadeQuickly() {
    for (List<LightningFlash.Stroke> strokes : planMany(2000)) {
      assertEquals(1, strokes.get(0).amplitude());
      assertEquals(0, strokes.get(0).start());
      for (int i = 0; i < strokes.size(); i++) {
        LightningFlash.Stroke stroke = strokes.get(i);
        if (i > 0) {
          assertTrue(stroke.amplitude() >= LightningFlash.MIN_LATER_STROKE);
          assertTrue(stroke.amplitude() <= LightningFlash.MAX_LATER_STROKE);
        }
        assertTrue(stroke.decay() >= LightningFlash.MIN_DECAY_SECONDS);
        assertTrue(stroke.decay() <= LightningFlash.MAX_DECAY_SECONDS);
      }
    }
  }

  @Test
  void aboutOneFlashInThreeHasAContinuingCurrent() {
    int glowing = 0;
    for (List<LightningFlash.Stroke> strokes : planMany(5000)) {
      int glows = 0;
      for (LightningFlash.Stroke stroke : strokes) {
        if (stroke.glow() > 0) {
          glows++;
          assertTrue(stroke.glow() >= LightningFlash.MIN_GLOW * stroke.amplitude() - 1e-12);
          assertTrue(stroke.glow() <= LightningFlash.MAX_GLOW * stroke.amplitude() + 1e-12);
          assertTrue(stroke.glowSeconds() >= LightningFlash.MIN_GLOW_SECONDS);
          assertTrue(stroke.glowSeconds() <= LightningFlash.MAX_GLOW_SECONDS);
        }
      }
      assertTrue(glows <= 1, "at most one continuing current per flash");
      glowing += glows;
    }
    double share = glowing / 5000.0;
    assertTrue(Math.abs(share - LightningFlash.CONTINUING_CURRENT_CHANCE) < 0.03, "share " + share);
  }

  @Test
  void aStrokeJumpsToItsPeakHoldsThenFadesExponentially() {
    LightningFlash.Stroke stroke = new LightningFlash.Stroke(0.1, 0.8, 0.04, 0, 0);
    assertEquals(0, stroke.brightnessAt(0.09));
    assertEquals(0.8, stroke.brightnessAt(0.1), "straight to the peak");
    assertEquals(0.8, stroke.brightnessAt(0.1 + LightningFlash.PEAK_SECONDS * 0.99));
    double oneDecayLater = 0.1 + LightningFlash.PEAK_SECONDS + 0.04;
    assertEquals(0.8 * Math.exp(-1), stroke.brightnessAt(oneDecayLater), 1e-9);
    assertTrue(stroke.brightnessAt(stroke.end() - 1e-9) < 0.001);
    assertEquals(0, stroke.brightnessAt(stroke.end()), "finished, not glowing forever");
  }

  @Test
  void aContinuingCurrentLingersThenFades() {
    LightningFlash.Stroke stroke = new LightningFlash.Stroke(0, 1, 0.03, 0.4, 0.2);
    double glowing = LightningFlash.PEAK_SECONDS + 0.15;
    assertEquals(0.4, stroke.brightnessAt(glowing), 1e-9, "the glow holds");
    double faded = LightningFlash.PEAK_SECONDS + 0.2 + 0.03;
    assertEquals(0.4 * Math.exp(-1), stroke.brightnessAt(faded), 1e-6, "then fades");
  }

  @Test
  void aFlashJumpsStraightToItsStrengthAndFlickersThroughItsStrokes() {
    LightningFlash lightning = new LightningFlash(5);
    while (lightning.upcomingStrokes().size() < 3) {
      lightning.strikeNow();
    }
    int strokes = lightning.upcomingStrokes().size();
    lightning.strikeNow();
    double full = lightning.strikeIntensity();
    assertEquals(full, lightning.brightness(), "full at once, no ramp up");
    assertTrue(lightning.isBright());
    int peaks = 1;
    double previous = lightning.brightness();
    for (int i = 0; i < 60; i++) {
      lightning.update(FRAME);
      double now = lightning.brightness();
      if (now > previous + 1e-9) {
        peaks++;
      }
      previous = now;
    }
    assertTrue(peaks >= Math.min(strokes, 2), "it flickers: " + peaks + " peaks");
    lightning.update(5);
    assertEquals(0, lightning.brightness(), 1e-9);
    assertTrue(!lightning.isBright());
  }

  @Test
  void eachFlashComesFromTheDirectionAndStrengthChosenAhead() {
    LightningFlash lightning = new LightningFlash(4);
    double angle = lightning.upcomingAngle();
    double strength = lightning.upcomingIntensity();
    assertTrue(angle >= 0 && angle < 2 * Math.PI);
    lightning.strikeNow();
    assertEquals(angle, lightning.strikeAngle());
    assertEquals(strength, lightning.strikeIntensity());
    assertTrue(lightning.upcomingAngle() != angle, "the next one is chosen at once");
  }

  @Test
  void flashesVaryFromThreeQuartersToFullStrength() {
    LightningFlash lightning = new LightningFlash(11);
    double weakest = 1;
    double strongest = 0;
    for (int i = 0; i < 400; i++) {
      double upcoming = lightning.upcomingIntensity();
      lightning.strikeNow();
      assertEquals(upcoming, lightning.brightness(), 1e-9);
      weakest = Math.min(weakest, upcoming);
      strongest = Math.max(strongest, upcoming);
    }
    assertTrue(
        weakest >= LightningFlash.MIN_INTENSITY && weakest < LightningFlash.MIN_INTENSITY + 0.02,
        "weakest " + weakest);
    assertTrue(strongest <= 1 && strongest > 0.95, "strongest " + strongest);
  }

  @Test
  void aDueFlashWaitsUntilReady() {
    LightningFlash lightning = new LightningFlash(4);
    lightning.setActive(true);
    lightning.setReady(false);
    for (int i = 0; i < 60 * 30; i++) {
      lightning.update(FRAME);
      assertEquals(0, lightning.brightness(), "no flash while not ready");
    }
    assertEquals(0, lightning.secondsUntilStrike(), "it is due, and waiting");
    lightning.setReady(true);
    lightning.update(FRAME);
    assertEquals(lightning.strikeIntensity(), lightning.brightness());
    lightning.setActive(false);
    lightning.strikeSoon();
    assertEquals(Double.POSITIVE_INFINITY, lightning.secondsUntilStrike(), "off until active");
  }
}
