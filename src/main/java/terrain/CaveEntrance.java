package terrain;

/**
 * A way down into a cave: an opening in the ground spanning a small rectangle of tiles, all at the
 * same height.
 *
 * <p>Entrances are placed by the terrain ({@link Terrain#caveEntranceAt}); what the opening looks
 * like is up to whoever draws it. Each one has its own {@link #seed}, from which the cave below it
 * can be generated, so the same entrance always leads to the same cave.
 *
 * @param minX the left column of tiles it covers, in blocks
 * @param minY the bottom row of tiles it covers, in blocks
 * @param widthBlocks how many columns of tiles it covers
 * @param heightBlocks how many rows of tiles it covers
 * @param seed seeds the cave this entrance leads to, and the opening's exact shape
 */
public record CaveEntrance(int minX, int minY, int widthBlocks, int heightBlocks, long seed) {

  /**
   * Creates an entrance.
   *
   * @throws IllegalArgumentException if it covers no tiles
   */
  public CaveEntrance {
    if (widthBlocks < 1 || heightBlocks < 1) {
      throw new IllegalArgumentException(
          "an entrance covers at least one tile: " + widthBlocks + " by " + heightBlocks);
    }
  }

  /**
   * Returns whether the entrance covers a tile.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @return {@code true} if the tile is within the entrance's rectangle
   */
  public boolean covers(int blockX, int blockY) {
    return blockX >= minX
        && blockX < minX + widthBlocks
        && blockY >= minY
        && blockY < minY + heightBlocks;
  }

  /**
   * Returns the middle of the entrance in the world, left to right.
   *
   * @return the x position, in world pixels
   */
  public double centreX() {
    return (minX + widthBlocks / 2.0) * TerrainCoordinates.BLOCK_SIZE_PX;
  }

  /**
   * Returns the middle of the entrance in the world, up and down.
   *
   * @return the y position, in world pixels
   */
  public double centreY() {
    return (minY + heightBlocks / 2.0) * TerrainCoordinates.BLOCK_SIZE_PX;
  }

  /**
   * Returns how far a point is from the middle of the entrance, for example to tell whether the
   * player is close enough to go in.
   *
   * @param worldX the point's x position, in world pixels
   * @param worldY the point's y position, in world pixels
   * @return the distance, in world pixels
   */
  public double distanceTo(double worldX, double worldY) {
    return Math.hypot(worldX - centreX(), worldY - centreY());
  }
}
