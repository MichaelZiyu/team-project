package terrainscene;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import terrain.Terrain;
import terrain.TerrainCoordinates;
import terrain.Tree;
import terrain.Trees;

/**
 * Works out the shadows raised terrain casts for a position of the sun.
 *
 * <p>Each tile taller than its surroundings throws a shadow away from the light. For every level
 * of height difference the shadow reaches one {@link TerrainLight} step further, for example 32 px
 * left and 16 px south for {@link TerrainLight#DEFAULT}, a sun to the right and a little north. The
 * shadow is the tall tile's footprint stretched along that direction; the part that lands on a
 * lower tile's top shades it. This class only does the geometry; it draws nothing.
 *
 * <p>Trees cast shadows too, block by block. A column of a tree's blocks that starts above the
 * ground, such as a canopy's edge, casts a shadow that starts away from its own footprint: its
 * square swept from where the column's bottom throws it to where its top does.
 *
 * <p>A real sun is not a point, so shadows are sharp next to the wall casting them and blur
 * further away. {@link #softShadowOn} imitates this by casting the same shadow from several
 * slightly different sun positions; where only some of them overlap is the soft edge.
 */
public final class SunShadows {

  /**
   * How far left one level of height throws its shadow under {@link TerrainLight#DEFAULT}, in
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

  /** Each tree shape's columns of blocks, as {@code {east, north, bottom, top}} in levels. */
  private static final Map<Tree.Shape, List<int[]>> TREE_COLUMNS = treeColumns();

  /** The most levels any tree reaches above its ground. */
  private static final int TALLEST_TREE =
      Arrays.stream(Tree.Shape.values()).mapToInt(Tree.Shape::topLevel).max().orElse(0);

  private final Terrain terrain;
  private final Trees trees;

  /**
   * Creates shadow geometry for some terrain, with no trees.
   *
   * @param terrain the terrain whose heights cast the shadows
   */
  public SunShadows(Terrain terrain) {
    this(terrain, null);
  }

  /**
   * Creates shadow geometry for some terrain and the trees growing on it.
   *
   * @param terrain the terrain whose heights cast the shadows
   * @param trees the trees, which cast shadows too; {@code null} for none
   */
  public SunShadows(Terrain terrain, Trees trees) {
    this.terrain = terrain;
    this.trees = trees;
  }

  /**
   * Returns the part of a tile's top that lies in shadow under the default sun.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @return the shaded region, clipped to the tile; empty if the tile is fully lit
   * @see #shadowOn(int, int, TerrainLight)
   */
  public Area shadowOn(int blockX, int blockY) {
    return shadowOn(blockX, blockY, TerrainLight.DEFAULT);
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
  public Area shadowOn(int blockX, int blockY, TerrainLight sun) {
    return shadowFrom(casters(blockX, blockY, sun), sun, 1, 0);
  }

  /**
   * Returns a tile's soft shadow under the default sun.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @return nine regions, as for {@link #softShadowOn(int, int, TerrainLight)}
   */
  public List<Area> softShadowOn(int blockX, int blockY) {
    return softShadowOn(blockX, blockY, TerrainLight.DEFAULT);
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
  public List<Area> softShadowOn(int blockX, int blockY, TerrainLight sun) {
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
   * Returns everything nearby that may shade a tile, as {@code {east, north, bottom, top}}: how
   * many tiles east and north a column is, and how many levels above the tile its bottom and top
   * are. A taller tile is a column from the tile's own level up; a column of a tree's blocks can
   * start higher.
   */
  private List<int[]> casters(int blockX, int blockY, TerrainLight sun) {
    List<int[]> casters = new ArrayList<>();
    double length = Math.hypot(sun.dxPerLevel(), sun.dyPerLevel());
    if (!sun.castsShadows()) {
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
          casters.add(new int[] {east, north, 0, rise});
        }
      }
    }
    addTreeCasters(casters, blockX, blockY, height, sun, length);
    return casters;
  }

  /** Adds the columns of every tree close enough to shade a tile whose top is at {@code height}. */
  private void addTreeCasters(
      List<int[]> casters, int blockX, int blockY, int height, TerrainLight sun, double length) {
    if (trees == null) {
      return;
    }
    // The furthest a tree's shadow reaches, in tiles: its full height's shadow, plus its canopy.
    double tilesPerLevel = length / BLOCK;
    int reach =
        (int) Math.ceil(TALLEST_TREE * tilesPerLevel) + Tree.Shape.BROAD.radius() + 1;
    double towardSunX = -sun.dxPerLevel() / length;
    double towardSunY = -sun.dyPerLevel() / length;
    for (Tree tree : trees.within(blockX - reach, blockY - reach, blockX + reach, blockY + reach)) {
      int lift = tree.groundHeight() - height;
      for (int[] column : TREE_COLUMNS.get(tree.shape())) {
        int east = tree.blockX() - blockX + column[0];
        int north = tree.blockY() - blockY + column[1];
        int bottom = Math.max(0, column[2] + lift);
        int top = column[3] + lift;
        // A column's shadow lands between where its bottom and its top throw it, toward the sun
        // from here; skip columns whose shadow cannot reach this tile at all.
        double along = east * towardSunX + north * towardSunY;
        double sideways = Math.abs(east * towardSunY - north * towardSunX);
        if (top > 0
            && sideways <= MAX_CASTER_SIDEWAYS
            && along >= bottom * tilesPerLevel * (1 - SOFT_LENGTH_SPREAD) - 1.5
            && along <= top * tilesPerLevel * (1 + SOFT_LENGTH_SPREAD) + 1.5) {
          casters.add(new int[] {east, north, bottom, top});
        }
      }
    }
  }

  /**
   * Splits each tree shape into columns: for each place a tree has blocks, each unbroken run of
   * them, from the bottom of its lowest block to the top of its highest, in levels above the
   * ground.
   */
  private static Map<Tree.Shape, List<int[]>> treeColumns() {
    Map<Tree.Shape, List<int[]>> columns = new EnumMap<>(Tree.Shape.class);
    for (Tree.Shape shape : Tree.Shape.values()) {
      Map<List<Integer>, TreeSet<Integer>> levels = new HashMap<>();
      for (Tree.Block block : shape.blocks()) {
        levels
            .computeIfAbsent(List.of(block.dx(), block.dy()), place -> new TreeSet<>())
            .add(block.level());
      }
      List<int[]> runs = new ArrayList<>();
      levels.forEach(
          (place, stack) -> {
            int start = stack.first();
            int previous = start;
            for (int level : stack.tailSet(start, false)) {
              if (level != previous + 1) {
                runs.add(new int[] {place.get(0), place.get(1), start - 1, previous});
                start = level;
              }
              previous = level;
            }
            runs.add(new int[] {place.get(0), place.get(1), start - 1, previous});
          });
      columns.put(shape, List.copyOf(runs));
    }
    return columns;
  }

  /**
   * Returns the union of the casters' shadows, clipped to the tile, for shadows {@code length}
   * times as long as {@code sun} makes them and turned by {@code turn} radians.
   */
  private static Area shadowFrom(
      List<int[]> casters, TerrainLight sun, double length, double turn) {
    double cos = Math.cos(turn);
    double sin = Math.sin(turn);
    Area shadow = new Area();
    double dx = sun.dxPerLevel() * length;
    double dy = sun.dyPerLevel() * length;
    double turnedX = dx * cos - dy * sin;
    double turnedY = dx * sin + dy * cos;
    for (int[] caster : casters) {
      int bottom = caster[2];
      int top = caster[3];
      Path2D cast =
          castShape(
              caster[0] * BLOCK,
              caster[1] * BLOCK,
              bottom * turnedX,
              bottom * turnedY,
              top * turnedX,
              top * turnedY);
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
   * the square swept from where its column's bottom throws it, {@code (fromX, fromY)} away, to
   * where its top does, {@code (toX, toY)} away. That is the outline around the two shifted
   * copies: a six-sided shape, or a rectangle when the sweep is straight along an axis.
   */
  private static Path2D castShape(
      int left, int bottom, double fromX, double fromY, double toX, double toY) {
    double right = left + BLOCK;
    double top = bottom + BLOCK;
    double[][] corners = {
      {left + fromX, bottom + fromY},
      {right + fromX, bottom + fromY},
      {right + fromX, top + fromY},
      {left + fromX, top + fromY},
      {left + toX, bottom + toY},
      {right + toX, bottom + toY},
      {right + toX, top + toY},
      {left + toX, top + toY}
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
