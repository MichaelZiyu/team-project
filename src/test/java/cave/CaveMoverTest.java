package cave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CaveMoverTest {

  private static final int BLOCK = CaveMover.BLOCK_PX;
  private static final double FRAME = 1.0 / 60;

  private static final CaveMover.Input NONE = CaveMover.Input.NONE;
  private static final CaveMover.Input LEFT = new CaveMover.Input(true, false, false, false, false);
  private static final CaveMover.Input RIGHT =
      new CaveMover.Input(false, true, false, false, false);
  private static final CaveMover.Input RUN_RIGHT =
      new CaveMover.Input(false, true, false, false, true);
  private static final CaveMover.Input UP = new CaveMover.Input(false, false, true, false, false);
  private static final CaveMover.Input DOWN = new CaveMover.Input(false, false, false, true, false);
  private static final CaveMover.Input UP_RIGHT =
      new CaveMover.Input(false, true, true, false, false);

  /** Rock placed block by block; a floor along row 0. */
  private static final class Rock implements CaveMover.Rock {
    private final Set<String> solid = new HashSet<>();

    Rock() {
      for (int x = -50; x <= 50; x++) {
        add(x, 0);
      }
    }

    Rock add(int x, int y) {
      solid.add(x + "," + y);
      return this;
    }

    /** A wall in one column, from row 1 up to a height. */
    Rock wall(int x, int height) {
      for (int y = 1; y <= height; y++) {
        add(x, y);
      }
      return this;
    }

    @Override
    public boolean isSolid(int blockX, int blockY) {
      return solid.contains(blockX + "," + blockY);
    }
  }

  private static void run(CaveMover mover, CaveMover.Input input, double seconds) {
    for (double t = 0; t < seconds; t += FRAME) {
      mover.update(FRAME, input);
    }
  }

  private static CaveMover standingOn(Rock rock, double x) {
    CaveMover mover = new CaveMover(rock, x, BLOCK);
    run(mover, NONE, 0.1);
    return mover;
  }

  @Test
  void bodyInTheAirFallsAndLandsOnTheFloor() {
    CaveMover mover = new CaveMover(new Rock(), 32, 5 * BLOCK);
    run(mover, NONE, 2);
    assertEquals(BLOCK, mover.feetY(), 1e-9);
    assertTrue(mover.isGrounded());
  }

  @Test
  void walkingMovesAtWalkingSpeedAndRunningFaster() {
    CaveMover walker = standingOn(new Rock(), 32);
    run(walker, RIGHT, 1);
    CaveMover runner = standingOn(new Rock(), 32);
    run(runner, RUN_RIGHT, 1);
    assertEquals(32 + CaveMover.WALK_PX_PER_SECOND, walker.feetX(), 5);
    assertEquals(32 + CaveMover.WALK_PX_PER_SECOND * CaveMover.RUN_FACTOR, runner.feetX(), 8);
  }

  @Test
  void wallStopsTheBody() {
    CaveMover mover = standingOn(new Rock().wall(3, 4), 32);
    run(mover, RIGHT, 3);
    assertEquals(3 * BLOCK - CaveMover.WIDTH_PX / 2, mover.feetX(), 1e-9);
  }

  @Test
  void jumpClearsOneBlockButNotTwo() {
    CaveMover overOne = standingOn(new Rock().wall(2, 1), 96);
    run(overOne, UP, 0.05);
    run(overOne, RIGHT, 1.5);
    assertTrue(overOne.feetX() > 3 * BLOCK, "cleared a one-block step: " + overOne.feetX());

    CaveMover againstTwo = standingOn(new Rock().wall(2, 2), 96);
    run(againstTwo, UP, 0.05);
    run(againstTwo, RIGHT, 1.5);
    assertTrue(againstTwo.feetX() < 2 * BLOCK, "stopped by a two-block wall: " + againstTwo.feetX());
  }

  @Test
  void holdingUpTowardTheWallClimbsIt() {
    CaveMover mover = standingOn(new Rock().wall(2, 5), 96);
    run(mover, RIGHT, 1);
    run(mover, UP_RIGHT, 1);
    assertTrue(mover.isClimbing());
    assertEquals(CaveMover.Pose.CLIMBING_RIGHT, mover.pose());
    assertEquals(BLOCK + CaveMover.CLIMB_PX_PER_SECOND, mover.feetY(), 10);
  }

  @Test
  void letGoOfTheKeysAndTheBodyHangsOnTheWall() {
    CaveMover mover = standingOn(new Rock().wall(2, 5), 96);
    run(mover, RIGHT, 1);
    run(mover, UP_RIGHT, 1);
    double height = mover.feetY();
    run(mover, NONE, 1);
    assertTrue(mover.isClimbing());
    assertEquals(height, mover.feetY(), 1e-9);
  }

  @Test
  void theDirectionAwayFromTheWallLetsGo() {
    CaveMover mover = standingOn(new Rock().wall(2, 5), 96);
    run(mover, RIGHT, 1);
    run(mover, UP_RIGHT, 1.5);
    run(mover, LEFT, 0.05);
    assertFalse(mover.isClimbing());
    run(mover, NONE, 2);
    assertEquals(BLOCK, mover.feetY(), 1e-9);
  }

  @Test
  void downClimbsDownAndLetsGoAtTheBottom() {
    CaveMover mover = standingOn(new Rock().wall(2, 5), 96);
    run(mover, RIGHT, 1);
    run(mover, UP_RIGHT, 1);
    run(mover, DOWN, 2);
    assertFalse(mover.isClimbing());
    assertEquals(BLOCK, mover.feetY(), 1e-9);
  }

  @Test
  void climbingPastTheTopPullsTheBodyOntoTheLedge() {
    CaveMover mover = standingOn(new Rock().wall(2, 3).wall(3, 3).wall(4, 3), 96);
    run(mover, RIGHT, 1);
    // Three blocks at the climbing speed, then a moment to step over the edge.
    run(mover, UP_RIGHT, 3 * BLOCK / CaveMover.CLIMB_PX_PER_SECOND + 0.4);
    run(mover, NONE, 1);
    assertTrue(mover.isGrounded(), "standing on top");
    assertEquals(4 * BLOCK, mover.feetY(), 1e-9);
    assertTrue(mover.feetX() > 2 * BLOCK && mover.feetX() < 5 * BLOCK, "on the ledge: " + mover.feetX());
  }

  @Test
  void ceilingStopsTheJump() {
    Rock rock = new Rock();
    for (int x = -5; x <= 5; x++) {
      rock.add(x, 2);
    }
    CaveMover mover = standingOn(rock, 32);
    double highest = 0;
    mover.update(FRAME, UP);
    for (int i = 0; i < 60; i++) {
      mover.update(FRAME, NONE);
      highest = Math.max(highest, mover.feetY());
    }
    assertTrue(highest + CaveMover.HEIGHT_PX <= 2 * BLOCK + 1e-6, "head stays under the ceiling");
  }

  @Test
  void poseFacesTheWayTheBodyWalksAndForwardWhenStill() {
    CaveMover mover = standingOn(new Rock(), 32);
    mover.update(FRAME, LEFT);
    assertEquals(CaveMover.Pose.LEFT, mover.pose());
    assertTrue(mover.isMoving());
    mover.update(FRAME, RIGHT);
    assertEquals(CaveMover.Pose.RIGHT, mover.pose());
    mover.update(FRAME, NONE);
    assertEquals(CaveMover.Pose.FORWARD, mover.pose());
    assertFalse(mover.isMoving());
  }

  @Test
  void fastFallNeverPassesThroughThinFloors() {
    Rock rock = new Rock();
    rock.add(0, 10);
    CaveMover mover = new CaveMover(rock, 32, 60 * BLOCK);
    run(mover, NONE, 1);
    mover.update(0.1, NONE);
    run(mover, NONE, 3);
    assertEquals(11 * BLOCK, mover.feetY(), 1e-9);
  }
}
