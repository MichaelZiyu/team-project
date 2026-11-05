package terrainscene;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import terrain.Terrain;
import terrain.TerrainCoordinates;

/**
 * Works out the shadows raised terrain casts, for a sun shining from the right of the screen and
 * a little from the north (the top of the screen).
 *
 * <p>Each tile taller than its surroundings throws a shadow away from the sun: to the left and a
 * little south. For every level of height difference the shadow reaches {@link
 * #LEFT_PER_LEVEL_PX} further left and {@link #SOUTH_PER_LEVEL_PX} further south. The shadow is
 * the tall tile's footprint stretched along that direction; the part that lands on a lower tile's
 * top shades it. This class only does the geometry; it draws nothing.
 *
 * <p>A real sun is not a point, so shadows are sharp next to the wall casting them and blur
 * further away. {@link #softShadowOn} imitates this by casting the same shadow from several
 * slightly different sun positions; where only some of them overlap is the soft edge.
 */
public final class SunShadows {

  /**
   * How far left one level of height throws its shadow, in pixels. Equal to a terrace step's
   * height, which is a sun 45 degrees above the horizon. A later time-of-day feature can vary it.
   */
  static final int LEFT_PER_LEVEL_PX = TileRenderable.STEP_HEIGHT_PX;

  /** How far south (toward the camera) one level of height throws its shadow, in pixels. */
  static final int SOUTH_PER_LEVEL_PX = TileRenderable.STEP_HEIGHT_PX / 2;

  /** How many tiles to the right to look for something tall enough to shade a tile. */
  static final int MAX_CASTER_DISTANCE_RIGHT = 8;

  /** How many tiles to the north to look for something tall enough to shade a tile. */
  static final int MAX_CASTER_DISTANCE_NORTH = 4;

  /** How much shorter or longer the soft shadow's outer samples are than the main shadow. */
  static final double SOFT_LENGTH_SPREAD = 0.15;

  /** How far the soft shadow's outer samples turn from the main shadow, in degrees. */
  static final double SOFT_ANGLE_SPREAD_DEGREES = 6;

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  private static final Rectangle2D TILE = new Rectangle2D.Double(0, 0, BLOCK, BLOCK);

  private final Terrain terrain;

  /**
   * Creates shadow geometry for some terrain.
   *
   * @param terrain the terrain whose heights cast the shadows
   */
  public SunShadows(Terrain terrain) {
    this.terrain = terrain;
  }

  /**
   * Returns the part of a tile's top that lies in shadow.
   *
   * <p>Coordinates are local to the tile, in pixels: {@code (0, 0)} is its lower-left corner and
   * {@code (64, 64)} its upper-right, with y increasing upward (north), like the world.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @return the shaded region, clipped to the tile; empty if the tile is fully lit
   */
  public Area shadowOn(int blockX, int blockY) {
    return shadowFrom(casters(blockX, blockY), 1, 0);
  }

  /**
   * Returns a tile's shadow cast from several sun positions around the real one, for drawing a
   * shadow with soft edges.
   *
   * <p>Each region is in the same coordinates as {@link #shadowOn}. The first is exactly {@code
   * shadowOn}; the others are slightly shorter, longer, or turned. Drawing each one faintly on top
   * of the others makes the shadow darkest where they all agree and fade where they do not.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @return nine regions, each clipped to the tile; all empty if the tile is fully lit
   */
  public List<Area> softShadowOn(int blockX, int blockY) {
    List<int[]> casters = casters(blockX, blockY);
    double angle = Math.toRadians(SOFT_ANGLE_SPREAD_DEGREES);
    double[] lengths = {1, 1 - SOFT_LENGTH_SPREAD, 1 + SOFT_LENGTH_SPREAD};
    double[] angles = {0, -angle, angle};
    List<Area> samples = new ArrayList<>();
    for (double length : lengths) {
      for (double turn : angles) {
        samples.add(shadowFrom(casters, length, turn));
      }
    }
    return samples;
  }

  /**
   * Returns every nearby tile taller than the given one, toward the sun, as {@code {right, north,
   * rise}}: how many tiles right and north it is, and how many levels taller.
   */
  private List<int[]> casters(int blockX, int blockY) {
    int height = terrain.heightAt(blockX, blockY);
    List<int[]> casters = new ArrayList<>();
    for (int right = 0; right <= MAX_CASTER_DISTANCE_RIGHT; right++) {
      for (int north = 0; north <= MAX_CASTER_DISTANCE_NORTH; north++) {
        if (right == 0 && north == 0) {
          continue;
        }
        int rise = terrain.heightAt(blockX + right, blockY + north) - height;
        if (rise > 0) {
          casters.add(new int[] {right, north, rise});
        }
      }
    }
    return casters;
  }

  /**
   * Returns the union of the casters' shadows, clipped to the tile, for a sun whose shadows are
   * {@code length} times as long as usual and turned by {@code turn} radians.
   */
  private static Area shadowFrom(List<int[]> casters, double length, double turn) {
    double cos = Math.cos(turn);
    double sin = Math.sin(turn);
    Area shadow = new Area();
    for (int[] caster : casters) {
      int rise = caster[2];
      double dx = -rise * LEFT_PER_LEVEL_PX * length;
      double dy = -rise * SOUTH_PER_LEVEL_PX * length;
      double turnedX = dx * cos - dy * sin;
      double turnedY = dx * sin + dy * cos;
      Path2D cast = castShape(caster[0] * BLOCK, caster[1] * BLOCK, turnedX, turnedY);
      // Building areas is slow, so skip shadows that cannot reach the tile at all.
      if (cast.intersects(TILE)) {
        shadow.add(new Area(cast));
      }
    }
    shadow.intersect(new Area(TILE));
    return shadow;
  }

  /**
   * Returns the shadow of a raised square whose lower-left corner is at {@code (left, bottom)}:
   * the square swept along {@code (dx, dy)}, which is a six-sided shape. Both {@code dx} and
   * {@code dy} must be negative (left and south), as they are for this sun.
   */
  private static Path2D castShape(int left, int bottom, double dx, double dy) {
    int right = left + BLOCK;
    int top = bottom + BLOCK;
    // The direction is left and down, so the shape runs from the square's top-right corner,
    // across its top and left side to the shifted copy, and back along its bottom and right.
    Path2D shape = new Path2D.Double();
    shape.moveTo(right, top);
    shape.lineTo(left, top);
    shape.lineTo(left + dx, top + dy);
    shape.lineTo(left + dx, bottom + dy);
    shape.lineTo(right + dx, bottom + dy);
    shape.lineTo(right, bottom);
    shape.closePath();
    return shape;
  }
}
