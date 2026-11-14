package weatherscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RainPerspectiveTest {

  private final RainPerspective perspective = new RainPerspective(800, 600);

  @Test
  void defaultsAreJustOffTheBottomWithAnOval() {
    assertEquals(RainPerspective.DEFAULT_VANISHING_OFFSET, perspective.getVanishingOffset());
    assertTrue(perspective.vanishingY() > 600, "off the bottom of the screen");
    assertEquals(RainPerspective.DEFAULT_OVAL_WIDTH, perspective.getOvalWidth());
  }

  @Test
  void vanishingPointIsCentredAndSlightlyBelowTheMiddle() {
    assertEquals(400, perspective.vanishingX(), 1e-9);
    assertEquals(300 + RainPerspective.DEFAULT_VANISHING_OFFSET * 600, perspective.vanishingY(), 1e-9);
    perspective.setVanishingOffset(0);
    assertEquals(300, perspective.vanishingY(), 1e-9);
    perspective.setVanishingOffset(1.5);
    assertEquals(300 + 1.5 * 600, perspective.vanishingY(), 1e-9);
    assertThrows(IllegalArgumentException.class, () -> perspective.setVanishingOffset(3.5));
  }

  @Test
  void withNoOvalBothDirectionsSpreadLikeTheScale() {
    perspective.setOvalWidth(0);
    assertEquals(perspective.scale(300), perspective.spreadX(300), 1e-9);
    assertEquals(perspective.scale(300), perspective.spreadY(300), 1e-9);
  }

  @Test
  void ovalIsTwiceAsWideAsTallAndWeakensTheSpread() {
    perspective.setOvalWidth(0.5);
    assertEquals(200, perspective.ovalRadiusX(), 1e-9);
    assertEquals(100, perspective.ovalRadiusY(), 1e-9);
    assertTrue(perspective.spreadX(300) < perspective.scale(300));
    assertTrue(perspective.spreadY(300) < perspective.scale(300));
    assertEquals(1, perspective.spreadX(0), 1e-9);
    assertThrows(IllegalArgumentException.class, () -> perspective.setOvalWidth(0.95));
  }

  @Test
  void rainAtTheEdgeHeadsForTheEdgeOfTheOval() {
    perspective.setOvalWidth(0.5);
    // Ground at the right edge of the screen, level with the oval's centre.
    double groundX = 800;
    double groundY = perspective.vanishingY();
    double[] high = perspective.project(groundX, groundY, 500);
    double[] higher = perspective.project(groundX, groundY, 600);
    // Moving down from higher to high and on to the ground, the drop heads toward the oval's
    // right edge, not its centre: extend its path and it passes through that edge point.
    double edgeX = perspective.vanishingX() + perspective.ovalRadiusX();
    double t = (higher[0] - edgeX) / (higher[0] - groundX);
    assertEquals(groundY, higher[1] + t * (groundY - higher[1]), 1e-9);
    assertTrue(high[0] > groundX && higher[0] > high[0]);
  }

  @Test
  void unprojectUndoesProjectWithAnOval() {
    perspective.setOvalWidth(0.7);
    perspective.setVanishingOffset(0.8);
    double[] screen = perspective.project(130, 90, 420);
    double[] ground = perspective.unproject(screen[0], screen[1], 420);
    assertEquals(130, ground[0], 1e-9);
    assertEquals(90, ground[1], 1e-9);
  }

  @Test
  void unprojectUndoesProject() {
    perspective.setVanishingOffset(1.2);
    double[] screen = perspective.project(130, 90, 420);
    double[] ground = perspective.unproject(screen[0], screen[1], 420);
    assertEquals(130, ground[0], 1e-9);
    assertEquals(90, ground[1], 1e-9);
  }

  @Test
  void groundMatchesTheGameCamera() {
    // The game's camera: x = objectX - playerX + width / 2, y = height / 2 - (objectY - playerY).
    double[] screen = perspective.groundToScreen(1100, 450, 1000, 500);
    assertEquals(500, screen[0], 1e-9);
    assertEquals(350, screen[1], 1e-9);
  }

  @Test
  void somethingOnTheGroundAppearsWhereTheGroundIs() {
    double[] onGround = perspective.project(120, 80, 0);
    assertEquals(120, onGround[0], 1e-9);
    assertEquals(80, onGround[1], 1e-9);
  }

  @Test
  void higherThingsArePushedAwayFromTheVanishingPointAlongTheSameLine() {
    perspective.setOvalWidth(0);
    double vx = perspective.vanishingX();
    double vy = perspective.vanishingY();
    double[] low = perspective.project(600, 100, 100);
    double[] high = perspective.project(600, 100, 500);
    double lowDistance = Math.hypot(low[0] - vx, low[1] - vy);
    double highDistance = Math.hypot(high[0] - vx, high[1] - vy);
    assertTrue(highDistance > lowDistance);
    // Same direction from the vanishing point: rain streaks point straight at it.
    double lowAngle = Math.atan2(low[1] - vy, low[0] - vx);
    double highAngle = Math.atan2(high[1] - vy, high[0] - vx);
    assertEquals(lowAngle, highAngle, 1e-9);
  }

  @Test
  void theVanishingPointItselfNeverMoves() {
    double[] above = perspective.project(perspective.vanishingX(), perspective.vanishingY(), 600);
    assertEquals(perspective.vanishingX(), above[0], 1e-9);
    assertEquals(perspective.vanishingY(), above[1], 1e-9);
  }
}
