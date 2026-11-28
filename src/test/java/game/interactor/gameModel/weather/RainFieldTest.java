package game.interactor.gameModel.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RainFieldTest {

  private static final double FRAME = 1.0 / 60;

  private final RainPerspective perspective = new RainPerspective(800, 600);

  RainFieldTest() {
    // A single vanishing point on screen, so the tests can check rain heads straight for it.
    perspective.setVanishingOffset(0.12);
    perspective.setOvalWidth(0);
  }

  private RainField field() {
    return new RainField(perspective, 800, 600, 3);
  }

  private static void run(RainField field, double seconds, double cameraX, double cameraY) {
    for (double t = 0; t < seconds; t += FRAME) {
      field.update(FRAME, cameraX, cameraY);
    }
  }

  @Test
  void clearSkyHasNothingToDraw() {
    RainField field = field();
    run(field, 2, 0, 0);
    assertTrue(field.isEmpty());
  }

  @Test
  void rainBuildsUpGraduallyToItsFullStrength() {
    RainField field = field();
    field.setStyle(RainStyle.RAIN);
    run(field, RainField.RAMP_SECONDS / 3, 0, 0);
    int early = field.dropCount();
    assertTrue(early > 0 && early < RainStyle.RAIN.drops(), "still building up: " + early);
    run(field, RainField.RAMP_SECONDS, 0, 0);
    assertEquals(RainStyle.RAIN.drops(), field.dropCount());
  }

  @Test
  void landingDropsLeaveSplashesThatFade() {
    RainField field = field();
    field.setStyle(RainStyle.RAIN);
    run(field, 2, 0, 0);
    assertTrue(field.splashCount() > 0);
    field.setStyle(RainStyle.NONE);
    run(field, 2, 0, 0);
    assertTrue(field.isEmpty(), "after rain stops, drops finish falling and splashes fade");
  }

  @Test
  void dropsStreamTowardTheVanishingPoint() {
    RainField field = new RainField(perspective, 800, 600, 3);
    field.setStyle(new RainStyle(200, 1400, 0, 0));
    run(field, 2, 0, 0);
    double vx = perspective.vanishingX();
    double vy = perspective.vanishingY();
    List<double[]> streaks = new ArrayList<>();
    field.forEachStreak(
        (headX, headY, tailX, tailY, nearness) ->
            streaks.add(new double[] {headX, headY, tailX, tailY, nearness}));
    // High drops appear beyond the screen's edges, so only some are in view at once.
    assertTrue(streaks.size() > 40, "drops in view: " + streaks.size());
    for (double[] s : streaks) {
      // With no wind, the head is nearer the vanishing point than the tail, on the same line.
      double head = Math.hypot(s[0] - vx, s[1] - vy);
      double tail = Math.hypot(s[2] - vx, s[3] - vy);
      assertTrue(head <= tail + 1e-9);
      double cross = (s[0] - vx) * (s[3] - vy) - (s[1] - vy) * (s[2] - vx);
      assertEquals(0, cross, 1e-6 * Math.max(1, head * tail));
      assertTrue(s[4] >= 0 && s[4] <= 1);
    }
  }

  @Test
  void streaksMatchThePerspectiveAndSkipWhatIsFarOffScreen() {
    RainField field = field();
    field.setStyle(new RainStyle(300, 1400, 0, 0));
    run(field, 2, 250, -40);
    field.forEachStreak(
        (headX, headY, tailX, tailY, nearness) -> {
          double border = FallingArea.MARGIN_PX;
          assertTrue(
              Math.max(headX, tailX) >= -border && Math.min(headX, tailX) <= 800 + border,
              "skipped if wholly outside the screen and its border");
          assertTrue(Math.max(headY, tailY) >= -border && Math.min(headY, tailY) <= 600 + border);
          // The head is on the line from the vanishing point through the tail.
          double vx = perspective.vanishingX();
          double vy = perspective.vanishingY();
          double cross = (headX - vx) * (tailY - vy) - (headY - vy) * (tailX - vx);
          assertEquals(0, cross, 1e-6 * Math.max(1, Math.hypot(headX - vx, headY - vy)
              * Math.hypot(tailX - vx, tailY - vy)));
        });
  }

  @Test
  void rainFollowsThePlayerAround() {
    RainField field = field();
    field.setStyle(RainStyle.RAIN);
    run(field, 2, 0, 0);
    run(field, 1, 5000, -3000);
    List<double[]> heads = new ArrayList<>();
    field.forEachStreak((hx, hy, tx, ty, n) -> heads.add(new double[] {hx, hy}));
    int onScreen = 0;
    for (double[] h : heads) {
      if (h[0] >= 0 && h[0] <= 800 && h[1] >= 0 && h[1] <= 600) {
        onScreen++;
      }
    }
    assertTrue(onScreen > heads.size() / 3, "rain is still around the player: " + onScreen);
  }

  @Test
  void rainCoversTheWholeScreenEvenWithTheVanishingPointOffScreen() {
    for (double offset : new double[] {0.12, 1.0, 2.5, -1.0}) {
      perspective.setVanishingOffset(offset);
      perspective.setOvalWidth(offset > 2 ? 0.8 : 0);
      RainField field = field();
      // Heavy rain, so each quarter of the screen has enough drops to compare.
      field.setStyle(new RainStyle(1100, 1400, 40, 0));
      run(field, 3, 0, 0);
      int[] quarters = new int[4];
      field.forEachStreak(
          (hx, hy, tx, ty, n) -> {
            if (hx >= 0 && hx < 800 && hy >= 0 && hy < 600) {
              quarters[(hy < 300 ? 0 : 2) + (hx < 400 ? 0 : 1)]++;
            }
          });
      int fewest = Math.min(Math.min(quarters[0], quarters[1]), Math.min(quarters[2], quarters[3]));
      int most = Math.max(Math.max(quarters[0], quarters[1]), Math.max(quarters[2], quarters[3]));
      String counts = java.util.Arrays.toString(quarters);
      assertTrue(fewest > 0.6 * most, "offset " + offset + ": " + counts);
    }
  }

  @Test
  void dropsStartJustOutOfViewSoNoneAppearMidScreen() {
    for (double[] setting : new double[][] {{0.96, 0.45}, {0.12, 0}, {2.0, 0.8}}) {
      perspective.setVanishingOffset(setting[0]);
      perspective.setOvalWidth(setting[1]);
      RainField field = field();
      java.util.Random random = new java.util.Random(9);
      int checked = 0;
      for (int i = 0; i < 2000; i++) {
        double groundX = -50 + random.nextDouble() * 900;
        double groundY = -50 + random.nextDouble() * 700;
        double height = field.startHeight(groundX, groundY);
        if (height >= RainField.MAX_HEIGHT_PX) {
          continue; // right under the vanishing point; such spots are picked again
        }
        double[] start = perspective.project(groundX, groundY, height);
        boolean outOfView = start[0] < 0 || start[0] > 800 || start[1] < 0 || start[1] > 600;
        assertTrue(outOfView, "starts in view at " + start[0] + ", " + start[1]);
        checked++;
      }
      assertTrue(checked > 1500);
    }
  }

  @Test
  void streaksAreShortDashes() {
    RainField field = field();
    field.setStyle(RainStyle.THUNDERSTORM);
    run(field, 2, 0, 0);
    field.forEachStreak(
        (hx, hy, tx, ty, n) ->
            assertTrue(Math.hypot(hx - tx, hy - ty) <= RainField.MAX_STREAK_PX + 1e-9));
  }

  /** Ground at one height everywhere, except for rows of tiles given their own height. */
  private static Ground ground(int height, int rowY, int rowHeight) {
    return (blockX, blockY) -> blockY == rowY ? rowHeight : height;
  }

  /** Lets rain fall for a while and returns every splash seen, as {x, y} on screen. */
  private static List<double[]> splashes(RainField field) {
    List<double[]> seen = new ArrayList<>();
    for (int frame = 0; frame < 180; frame++) {
      field.update(FRAME, 0, 0);
      field.forEachSplash((x, y, progress) -> seen.add(new double[] {x, y}));
    }
    return seen;
  }

  @Test
  void dropsLandOnRaisedTileTops() {
    // One drop, so both fields follow the same drop to the same spot.
    RainField flat = field();
    flat.setStyle(new RainStyle(1, 1400, 0, 0));
    RainField raised = field();
    raised.setGround(ground(2, Integer.MIN_VALUE, 0));
    raised.setStyle(new RainStyle(1, 1400, 0, 0));
    double[] onFlat = firstSplash(flat);
    double[] onRaised = firstSplash(raised);
    // It lands in the same place, but on a top drawn two steps (64 px) higher on screen.
    assertEquals(onFlat[0], onRaised[0], 1e-9);
    assertEquals(onFlat[1] - 64, onRaised[1], 1e-9);
  }

  /** Lets rain fall until the first splash appears, and returns it as {x, y} on screen. */
  private static double[] firstSplash(RainField field) {
    for (int frame = 0; frame < 600; frame++) {
      field.update(FRAME, 0, 0);
      List<double[]> seen = new ArrayList<>();
      field.forEachSplash((x, y, progress) -> seen.add(new double[] {x, y}));
      if (!seen.isEmpty()) {
        return seen.get(0);
      }
    }
    throw new AssertionError("no drop landed");
  }

  @Test
  void spotsHiddenBehindATallerTileInFrontGetNoSplash() {
    // Flat ground at height 0, with a tall row (height 6) at block row y = -1, just south of
    // the player's row: it is drawn lifted up the screen and hides the row behind it.
    RainField field = field();
    field.setGround(ground(0, -1, 6));
    field.setStyle(new RainStyle(600, 1400, 0, 0));
    run(field, 1, 0, 0);
    for (double[] splash : splashes(field)) {
      // Screen y of the ground of row 0 (world y 0 to 64) at camera y 0 is 300 down to 236.
      boolean onRowBehind = splash[1] <= 300 && splash[1] > 236;
      assertTrue(!onRowBehind, "a splash is drawn where the tall row covers the ground");
    }
  }

  @Test
  void sameSeedFallsTheSameWay() {
    RainField a = field();
    RainField b = field();
    a.setStyle(RainStyle.THUNDERSTORM);
    b.setStyle(RainStyle.THUNDERSTORM);
    run(a, 1, 10, 20);
    run(b, 1, 10, 20);
    List<Double> first = new ArrayList<>();
    List<Double> second = new ArrayList<>();
    a.forEachStreak((hx, hy, tx, ty, n) -> first.add(hx + hy));
    b.forEachStreak((hx, hy, tx, ty, n) -> second.add(hx + hy));
    assertEquals(first, second);
  }

  @Test
  void stylesRejectImpossibleValues() {
    assertThrows(IllegalArgumentException.class, () -> new RainStyle(-1, 1000, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> new RainStyle(10, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> RainStyle.RAIN.scaled(-1, 1));
    assertThrows(IllegalArgumentException.class, () -> RainStyle.RAIN.scaled(1, 0));
  }

  @Test
  void scalingChangesAmountAndSpeedButNotWind() {
    RainStyle style = new RainStyle(100, 1000, 40, -10).scaled(1.5, 2);
    assertEquals(150, style.drops());
    assertEquals(2000, style.fallSpeedPx(), 1e-9);
    assertEquals(40, style.windEastPx(), 1e-9);
    assertEquals(-10, style.windNorthPx(), 1e-9);
  }

  @Test
  void fasterRainLeavesLongerStreaks() {
    double slow = averageStreakLength(1);
    double fast = averageStreakLength(2);
    assertTrue(fast > 1.5 * slow, "slow " + slow + ", fast " + fast);
  }

  private double averageStreakLength(double speed) {
    RainField field = field();
    field.setStyle(new RainStyle(300, 1400, 0, 0).scaled(1, speed));
    run(field, 2, 0, 0);
    double[] total = {0, 0};
    field.forEachStreak(
        (hx, hy, tx, ty, n) -> {
          total[0] += Math.hypot(hx - tx, hy - ty);
          total[1]++;
        });
    return total[0] / total[1];
  }

  @Test
  void windBlownDropsFallInStraightLinesEvenWithAVanishingOval() {
    RainPerspective wide = new RainPerspective(800, 600);
    wide.setVanishingOffset(0.12);
    wide.setOvalWidth(0.45);
    RainField field = new RainField(wide, 800, 600, 9);
    // A single drop, in a strong wind, so any bend in its path would be large.
    field.setStyle(new RainStyle(1, 600, 400, 150));
    List<double[]> path = new ArrayList<>();
    for (int frame = 0; frame < 600; frame++) {
      field.update(FRAME, 0, 0);
      List<double[]> heads = new ArrayList<>();
      field.forEachStreak((hx, hy, tx, ty, nearness) -> heads.add(new double[] {hx, hy}));
      if (heads.size() != 1) {
        // Out of view: keep a whole fall already seen, or start looking again.
        if (path.size() >= 10) {
          break;
        }
        path.clear();
        continue;
      }
      double[] head = heads.get(0);
      if (!path.isEmpty()) {
        double[] last = path.get(path.size() - 1);
        if (Math.hypot(head[0] - last[0], head[1] - last[1]) > 150) {
          // The drop landed and started again somewhere else.
          if (path.size() >= 10) {
            break;
          }
          path.clear();
        }
      }
      path.add(head);
    }
    assertTrue(path.size() >= 10, "need one fall in view: " + path.size());
    double[] first = path.get(0);
    double[] last = path.get(path.size() - 1);
    double length = Math.hypot(last[0] - first[0], last[1] - first[1]);
    assertTrue(length > 50, "the fall should cross the screen: " + length);
    for (double[] point : path) {
      double offLine =
          Math.abs((last[0] - first[0]) * (point[1] - first[1])
              - (last[1] - first[1]) * (point[0] - first[0])) / length;
      assertTrue(offLine < 0.01, "bent by " + offLine + " px");
    }
  }
}
