package daynight;

import java.util.Objects;

/**
 * The game's sun, following the world's clock.
 *
 * <p>There is one per world, owned by the world and shared through it ({@code world.getSun()}),
 * so terrain, characters and anything else lit by the sun all agree on where it is. It is not a
 * static global: tests and tools can make their own sun from any {@link DayClock}.
 *
 * <p>To draw a shadow for something, ask for its offset with {@link #shadowX} and {@link
 * #shadowY}, passing its height, and draw it as dark as {@link #daylight} says: full in daylight,
 * fading at dawn and dusk, gone at night.
 */
public final class Sun {

  private final DayClock clock;

  /**
   * Creates the sun for a clock.
   *
   * @param clock the world's clock, which says where the sun is
   */
  public Sun(DayClock clock) {
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * Returns where the sun is right now.
   *
   * @return the sun's position for the clock's current time
   */
  public SunPosition position() {
    return SunPosition.at(clock.timeOfDay());
  }

  /**
   * Returns how strong sunlight is right now, which is also how dark shadows should be.
   *
   * @return 0 at night, rising through dawn to 1 in full daylight
   * @see SunPosition#daylight()
   */
  public double daylight() {
    return position().daylight();
  }

  /**
   * Returns how long the shadow of something of the given height is right now.
   *
   * @param height how tall the thing casting the shadow is, in any unit (for example pixels)
   * @return the shadow's length, in the same unit; 0 at night
   * @see SunPosition#shadowLength(double)
   */
  public double shadowLength(double height) {
    return position().shadowLength(height);
  }

  /**
   * Returns how far east the shadow of the top of something of the given height falls from its
   * base right now.
   *
   * @param height how tall the thing casting the shadow is, in any unit (for example pixels)
   * @return the east-west offset, in the same unit; negative is west, 0 at night
   * @see SunPosition#shadowX(double)
   */
  public double shadowX(double height) {
    return position().shadowX(height);
  }

  /**
   * Returns how far north the shadow of the top of something of the given height falls from its
   * base right now.
   *
   * @param height how tall the thing casting the shadow is, in any unit (for example pixels)
   * @return the north-south offset, in the same unit; negative is south, 0 at night
   * @see SunPosition#shadowY(double)
   */
  public double shadowY(double height) {
    return position().shadowY(height);
  }

  /**
   * Returns the clock this sun follows.
   *
   * @return the world's clock
   */
  public DayClock clock() {
    return clock;
  }
}
