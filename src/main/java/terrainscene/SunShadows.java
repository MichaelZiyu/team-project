package terrainscene;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
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
    int height = terrain.heightAt(blockX, blockY);
    Area shadow = new Area();
    for (int right = 0; right <= MAX_CASTER_DISTANCE_RIGHT; right++) {
      for (int north = 0; north <= MAX_CASTER_DISTANCE_NORTH; north++) {
        if (right == 0 && north == 0) {
          continue;
        }
        int rise = terrain.heightAt(blockX + right, blockY + north) - height;
        if (rise > 0) {
          shadow.add(new Area(castShape(right * BLOCK, north * BLOCK, rise)));
        }
      }
    }
    shadow.intersect(new Area(TILE));
    return shadow;
  }

  /**
   * Returns the shadow of a raised square whose lower-left corner is at {@code (left, bottom)}:
   * the square swept along the shadow direction, which is a six-sided shape.
   */
  private static Path2D castShape(int left, int bottom, int rise) {
    double dx = -rise * LEFT_PER_LEVEL_PX;
    double dy = -rise * SOUTH_PER_LEVEL_PX;
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
