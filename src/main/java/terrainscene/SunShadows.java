package terrainscene;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import terrain.Terrain;
import terrain.TerrainCoordinates;

/**
 * Works out the shadows raised terrain casts for a position of the sun.
 *
 * <p>Each tile taller than its surroundings throws a shadow away from the sun. For every level of
 * height difference the shadow reaches one {@link ShadowCast} step further, for example 32 px left
 * and 16 px south for {@link ShadowCast#DEFAULT}, a sun to the right and a little north. The
 * shadow is the tall tile's footprint stretched along that direction; the part that lands on a
 * lower tile's top shades it. This class only does the geometry; it draws nothing.
 *
 * <p>A real sun is not a point, so shadows are sharp next to the wall casting them and blur
 * further away. {@link #softShadowOn} imitates this by casting the same shadow from several
 * slightly different sun positions; where only some of them overlap is the soft edge.
 */
public final class SunShadows {

  /**
   * How far left one level of height throws its shadow under {@link ShadowCast#DEFAULT}, in
   * pixels. Equal to a terrace step's height, which is a sun 45 degrees above the horizon.
   */
  static final int LEFT_PER_LEVEL_PX = TileRenderable.STEP_HEIGHT_PX;

  /** How far south one level of height throws its shadow under the default sun, in pixels. */
  static final int SOUTH_PER_LEVEL_PX = TileRenderable.STEP_HEIGHT_PX / 2;

  /** How many tiles toward the sun to look for something tall enough to shade a tile. */
  static final int MAX_CASTER_DISTANCE = 8;

  /**
   * How far to the side of the line toward the sun a caster can be and still shade a tile, in
   * tiles. A tile is at most about 1.4 tiles wide diagonally; the rest allows for soft shadows.
   */
  static final double MAX_CASTER_SIDEWAYS = 2.5;

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
   * Returns the part of a tile's top that lies in shadow under the default sun.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @return the shaded region, clipped to the tile; empty if the tile is fully lit
   * @see #shadowOn(int, int, ShadowCast)
   */
  public Area shadowOn(int blockX, int blockY) {
    return shadowOn(blockX, blockY, ShadowCast.DEFAULT);
  }

  /**
   * Returns the part of a tile's top that lies in shadow.
   *
   * <p>Coordinates are local to the tile, in pixels: {@code (0, 0)} is its lower-left corner and
   * {@code (64, 64)} its upper-right, with y increasing upward (north), like the world.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @param sun how the sun casts shadows
   * @return the shaded region, clipped to the tile; empty if the tile is fully lit or it is night
   */
  public Area shadowOn(int blockX, int blockY, ShadowCast sun) {
    return shadowFrom(casters(blockX, blockY, sun), sun, 1, 0);
  }

  /**
   * Returns a tile's soft shadow under the default sun.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @return nine regions, as for {@link #softShadowOn(int, int, ShadowCast)}
   */
  public List<Area> softShadowOn(int blockX, int blockY) {
    return softShadowOn(blockX, blockY, ShadowCast.DEFAULT);
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
   * @param sun how the sun casts shadows
   * @return nine regions, each clipped to the tile; all empty if the tile is fully lit
   */
  public List<Area> softShadowOn(int blockX, int blockY, ShadowCast sun) {
    List<int[]> casters = casters(blockX, blockY, sun);
    double angle = Math.toRadians(SOFT_ANGLE_SPREAD_DEGREES);
    double[] lengths = {1, 1 - SOFT_LENGTH_SPREAD, 1 + SOFT_LENGTH_SPREAD};
    double[] angles = {0, -angle, angle};
    List<Area> samples = new ArrayList<>();
    for (double length : lengths) {
      for (double turn : angles) {
        samples.add(shadowFrom(casters, sun, length, turn));
      }
    }
    return samples;
  }

  /**
   * Returns every nearby tile taller than the given one that lies toward the sun, as {@code
   * {east, north, rise}}: how many tiles east and north it is, and how many levels taller.
   */
  private List<int[]> casters(int blockX, int blockY, ShadowCast sun) {
    List<int[]> casters = new ArrayList<>();
    double length = Math.hypot(sun.dxPerLevel(), sun.dyPerLevel());
    if (!sun.isDaylight() || length == 0) {
      return casters;
    }
    double towardSunX = -sun.dxPerLevel() / length;
    double towardSunY = -sun.dyPerLevel() / length;
    int height = terrain.heightAt(blockX, blockY);
    for (int east = -MAX_CASTER_DISTANCE; east <= MAX_CASTER_DISTANCE; east++) {
      for (int north = -MAX_CASTER_DISTANCE; north <= MAX_CASTER_DISTANCE; north++) {
        double along = east * towardSunX + north * towardSunY;
        double sideways = Math.abs(east * towardSunY - north * towardSunX);
        if ((east == 0 && north == 0)
            || along < 0
            || along > MAX_CASTER_DISTANCE
            || sideways > MAX_CASTER_SIDEWAYS) {
          continue;
        }
        int rise = terrain.heightAt(blockX + east, blockY + north) - height;
        if (rise > 0) {
          casters.add(new int[] {east, north, rise});
        }
      }
    }
    return casters;
  }

  /**
   * Returns the union of the casters' shadows, clipped to the tile, for shadows {@code length}
   * times as long as {@code sun} makes them and turned by {@code turn} radians.
   */
  private static Area shadowFrom(List<int[]> casters, ShadowCast sun, double length, double turn) {
    double cos = Math.cos(turn);
    double sin = Math.sin(turn);
    Area shadow = new Area();
    for (int[] caster : casters) {
      int rise = caster[2];
      double dx = rise * sun.dxPerLevel() * length;
      double dy = rise * sun.dyPerLevel() * length;
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
   * the square swept along {@code (dx, dy)}, which is the outline around the square and its
   * shifted copy (a six-sided shape, or a rectangle when the sweep is straight along an axis).
   */
  private static Path2D castShape(int left, int bottom, double dx, double dy) {
    double right = left + BLOCK;
    double top = bottom + BLOCK;
    double[][] corners = {
      {left, bottom},
      {right, bottom},
      {right, top},
      {left, top},
      {left + dx, bottom + dy},
      {right + dx, bottom + dy},
      {right + dx, top + dy},
      {left + dx, top + dy}
    };
    return outline(corners);
  }

  /** Returns the convex outline (hull) around some points, using Andrew's monotone chain. */
  private static Path2D outline(double[][] points) {
    double[][] sorted = points.clone();
    Arrays.sort(
        sorted, (a, b) -> a[0] != b[0] ? Double.compare(a[0], b[0]) : Double.compare(a[1], b[1]));
    double[][] hull = new double[2 * sorted.length][];
    int size = 0;
    for (double[] point : sorted) {
      while (size >= 2 && turn(hull[size - 2], hull[size - 1], point) <= 0) {
        size--;
      }
      hull[size++] = point;
    }
    int lowerSize = size + 1;
    for (int i = sorted.length - 2; i >= 0; i--) {
      while (size >= lowerSize && turn(hull[size - 2], hull[size - 1], sorted[i]) <= 0) {
        size--;
      }
      hull[size++] = sorted[i];
    }
    Path2D shape = new Path2D.Double();
    shape.moveTo(hull[0][0], hull[0][1]);
    // The last point repeats the first, so it is left out.
    for (int i = 1; i < size - 1; i++) {
      shape.lineTo(hull[i][0], hull[i][1]);
    }
    shape.closePath();
    return shape;
  }

  /** Returns which way the path a, b, c turns: positive for left, negative for right. */
  private static double turn(double[] a, double[] b, double[] c) {
    return (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
  }
}
