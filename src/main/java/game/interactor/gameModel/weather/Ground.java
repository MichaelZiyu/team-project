package game.interactor.gameModel.weather;

/**
 * The ground that rain and snow land on: how high each tile stands, so falling weather lands on
 * raised tiles' tops, and lands hidden behind taller tiles in front. The terrain provides it; until
 * then the ground is {@link #FLAT}.
 */
@FunctionalInterface
public interface Ground {

  /** Ground that is level everywhere, at height 0. */
  Ground FLAT = (blockX, blockY) -> 0;

  /** How wide and tall a tile is, in world pixels. */
  int BLOCK_PX = 64;

  /** How far up the screen each level of height lifts a tile's top, in pixels. */
  int STEP_PX = 32;

  /**
   * Returns how high a tile stands.
   *
   * @param blockX the tile's x coordinate, in tiles
   * @param blockY the tile's y coordinate, in tiles
   * @return its height, in levels; 0 at the lowest
   */
  int heightAt(int blockX, int blockY);

  /**
   * Returns which tile a world position is on, along one axis.
   *
   * @param worldPx a position in the world, in pixels
   * @return the tile's coordinate along that axis
   */
  static int tileAt(double worldPx) {
    return (int) Math.floor(worldPx / BLOCK_PX);
  }
}
