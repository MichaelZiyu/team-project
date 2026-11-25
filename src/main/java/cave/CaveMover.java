package cave;

/**
 * Moves a body through a side-view cave: walking, falling, jumping, and climbing walls.
 *
 * <p>Gravity pulls the body down. A/D walk left and right; W jumps from the ground. Holding W and
 * the direction toward a wall the body is beside grabs the wall and climbs it; on the wall, W
 * climbs up, S climbs down, nothing hangs still, and the direction away from the wall lets go.
 * Climbing past the top of a wall pulls the body up over the edge, onto the ledge.
 *
 * <p>The body is a box {@link #WIDTH_PX} wide and {@link #HEIGHT_PX} tall, placed by the middle of
 * its bottom edge, its feet, like the game's sprites. Rock stops it: it never overlaps a solid
 * block. Positions are in pixels, with each block {@link #BLOCK_PX} square and y pointing up.
 */
public final class CaveMover {

  /** What the player is pressing. */
  public record Input(boolean left, boolean right, boolean up, boolean down, boolean run) {

    /** Nothing pressed. */
    public static final Input NONE = new Input(false, false, false, false, false);

    /** Returns -1 for left, 1 for right, 0 for neither or both. */
    int across() {
      return (right ? 1 : 0) - (left ? 1 : 0);
    }
  }

  /** Where solid rock is. */
  @FunctionalInterface
  public interface Rock {
    /**
     * Returns whether a block is solid.
     *
     * @param blockX the block's x coordinate
     * @param blockY the block's y coordinate
     * @return {@code true} if it is rock
     */
    boolean isSolid(int blockX, int blockY);
  }

  /** How the body looks right now, for choosing its sprite. */
  public enum Pose {
    /** Standing or falling without moving sideways: facing the viewer. */
    FORWARD,
    /** Walking, running or jumping to the left. */
    LEFT,
    /** Walking, running or jumping to the right. */
    RIGHT,
    /** On a wall to its left, facing it. */
    CLIMBING_LEFT,
    /** On a wall to its right, facing it. */
    CLIMBING_RIGHT
  }

  /** How big a block is, in pixels. */
  public static final int BLOCK_PX = 64;

  /** How wide the body is, in pixels. */
  public static final double WIDTH_PX = 30;

  /** How tall the body is, in pixels. */
  public static final double HEIGHT_PX = 58;

  /** How fast the body walks, in pixels per second. */
  public static final double WALK_PX_PER_SECOND = 150;

  /** How much faster running is than walking. */
  public static final double RUN_FACTOR = 1.8;

  /** How fast gravity speeds a fall, in pixels per second per second. */
  public static final double GRAVITY = 1900;

  /** How fast a jump starts upward, in pixels per second: high enough to clear a block. */
  public static final double JUMP_PX_PER_SECOND = 600;

  /** The fastest the body falls, in pixels per second. */
  public static final double MAX_FALL_PX_PER_SECOND = 1100;

  /** How fast the body climbs up or down a wall, in pixels per second. */
  public static final double CLIMB_PX_PER_SECOND = 110;

  /** The longest step of time moved at once, in seconds, so fast falls never skip a block. */
  static final double MAX_STEP_SECONDS = 1.0 / 240;

  /** How close a wall must be to grab it, in pixels. */
  static final double REACH_PX = 2;

  private final Rock rock;
  private double feetX;
  private double feetY;
  private double velocityX;
  private double velocityY;
  private boolean grounded;
  private int clinging;
  private Pose pose = Pose.FORWARD;
  private boolean moving;

  /**
   * Creates a body standing at a place in a cave.
   *
   * @param rock where the cave's rock is
   * @param x the middle of the body's feet, in pixels
   * @param y the bottom of the body's feet, in pixels
   */
  public CaveMover(Rock rock, double x, double y) {
    this.rock = rock;
    this.feetX = x;
    this.feetY = y;
  }

  /**
   * Moves the body on in time, as the player presses.
   *
   * @param seconds how much time passed, in seconds
   * @param input what the player is pressing
   */
  public void update(double seconds, Input input) {
    double left = seconds;
    while (left > 0) {
      double step = Math.min(MAX_STEP_SECONDS, left);
      step(step, input);
      left -= step;
    }
    choosePose(input);
  }

  private void step(double seconds, Input input) {
    int across = input.across();
    if (clinging != 0) {
      climb(input, across);
    } else {
      double speed = WALK_PX_PER_SECOND * (input.run() ? RUN_FACTOR : 1);
      velocityX = across * speed;
      if (across != 0 && input.up() && touchingWall(across)) {
        clinging = across;
        velocityX = 0;
        velocityY = CLIMB_PX_PER_SECOND;
      } else if (input.up() && grounded) {
        velocityY = JUMP_PX_PER_SECOND;
        grounded = false;
      }
    }
    if (clinging == 0) {
      velocityY = Math.max(-MAX_FALL_PX_PER_SECOND, velocityY - GRAVITY * seconds);
    }
    moveAcross(velocityX * seconds);
    moveUp(velocityY * seconds);
  }

  /** Climbs, hangs, or lets go, while on a wall. */
  private void climb(Input input, int across) {
    velocityX = 0;
    if (across == -clinging) {
      // Pushing away from the wall lets go.
      clinging = 0;
      return;
    }
    if (!touchingWall(clinging)) {
      // The wall ended. Climbing up past its top pulls the body over the edge onto the ledge.
      boolean overTheTop = velocityY > 0;
      int side = clinging;
      clinging = 0;
      if (overTheTop) {
        velocityY = JUMP_PX_PER_SECOND * 0.45;
        velocityX = side * WALK_PX_PER_SECOND;
      }
      return;
    }
    velocityY = input.up() ? CLIMB_PX_PER_SECOND : input.down() ? -CLIMB_PX_PER_SECOND : 0;
    if (input.down() && grounded) {
      clinging = 0;
    }
  }

  /** Moves sideways, stopping against rock. */
  private void moveAcross(double distance) {
    if (distance == 0) {
      return;
    }
    feetX += distance;
    double bottom = feetY + 0.01;
    double top = feetY + HEIGHT_PX - 0.01;
    if (distance > 0) {
      int column = block(feetX + WIDTH_PX / 2 - 0.01);
      if (anySolid(column, bottom, top)) {
        feetX = column * BLOCK_PX - WIDTH_PX / 2;
        velocityX = 0;
      }
    } else {
      int column = block(feetX - WIDTH_PX / 2 + 0.01);
      if (anySolid(column, bottom, top)) {
        feetX = (column + 1) * BLOCK_PX + WIDTH_PX / 2;
        velocityX = 0;
      }
    }
  }

  /** Moves up or down, landing on rock below or bumping rock above. */
  private void moveUp(double distance) {
    feetY += distance;
    double leftEdge = feetX - WIDTH_PX / 2 + 0.01;
    double rightEdge = feetX + WIDTH_PX / 2 - 0.01;
    if (distance <= 0) {
      int row = block(feetY);
      if (anySolidAcross(row, leftEdge, rightEdge)) {
        feetY = (row + 1) * BLOCK_PX;
        velocityY = 0;
        grounded = true;
      } else {
        grounded = anySolidAcross(block(feetY - 0.5), leftEdge, rightEdge);
      }
    } else {
      grounded = false;
      int row = block(feetY + HEIGHT_PX - 0.01);
      if (anySolidAcross(row, leftEdge, rightEdge)) {
        feetY = row * BLOCK_PX - HEIGHT_PX;
        velocityY = 0;
      }
    }
  }

  /** Returns whether rock is right beside the body on one side, anywhere along its height. */
  boolean touchingWall(int side) {
    double edge = feetX + side * (WIDTH_PX / 2 + REACH_PX);
    return anySolid(block(edge), feetY + 1, feetY + HEIGHT_PX - 1);
  }

  private boolean anySolid(int column, double bottom, double top) {
    for (int row = block(bottom); row <= block(top); row++) {
      if (rock.isSolid(column, row)) {
        return true;
      }
    }
    return false;
  }

  private boolean anySolidAcross(int row, double leftEdge, double rightEdge) {
    for (int column = block(leftEdge); column <= block(rightEdge); column++) {
      if (rock.isSolid(column, row)) {
        return true;
      }
    }
    return false;
  }

  private void choosePose(Input input) {
    if (clinging != 0) {
      pose = clinging < 0 ? Pose.CLIMBING_LEFT : Pose.CLIMBING_RIGHT;
      moving = velocityY != 0;
      return;
    }
    int across = input.across();
    pose = across < 0 ? Pose.LEFT : across > 0 ? Pose.RIGHT : Pose.FORWARD;
    moving = across != 0 && velocityX != 0;
  }

  private static int block(double px) {
    return (int) Math.floor(px / BLOCK_PX);
  }

  /**
   * Returns the middle of the body's feet.
   *
   * @return the x coordinate, in pixels
   */
  public double feetX() {
    return feetX;
  }

  /**
   * Returns the bottom of the body's feet.
   *
   * @return the y coordinate, in pixels
   */
  public double feetY() {
    return feetY;
  }

  /**
   * Returns whether the body stands on rock.
   *
   * @return {@code true} if it is on the ground, not falling, jumping or climbing
   */
  public boolean isGrounded() {
    return grounded && clinging == 0;
  }

  /**
   * Returns whether the body is on a wall.
   *
   * @return {@code true} while climbing or hanging
   */
  public boolean isClimbing() {
    return clinging != 0;
  }

  /**
   * Returns how the body looks, for its sprite.
   *
   * @return the pose
   */
  public Pose pose() {
    return pose;
  }

  /**
   * Returns whether the body's limbs are moving: walking, or climbing up or down.
   *
   * @return {@code true} to animate the sprite
   */
  public boolean isMoving() {
    return moving;
  }
}
