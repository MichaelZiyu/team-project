package terrainscene;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import jswing.Texture;
import terrain.BlockType;
import terrain.CaveEntrance;
import terrain.TerrainCoordinates;

/**
 * One tile's part of a cave entrance's opening, lying on the tile's top. An entrance spans
 * several tiles; each draws its own piece of the same painting ({@link CaveEntrancePainter}), so
 * together they show the whole opening, under the shadows and light that fall on those tiles.
 */
public final class CaveEntranceRenderable implements TerrainPiece {

  private static final long serialVersionUID = 1L;

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  private final int blockX;
  private final int blockY;
  private final int height;
  private final CaveEntrance entrance;
  private final BlockType surface;

  /** Cut from the entrance's painting on first use, and never saved. */
  private transient BufferedImage image;

  /** Made from the image on first use, and never saved. */
  private transient Texture texture;

  /**
   * Creates one tile's part of an entrance.
   *
   * @param blockX the tile's x coordinate, in blocks; must be one the entrance covers
   * @param blockY the tile's y coordinate, in blocks; must be one the entrance covers
   * @param height the tile's terrace height, so the opening lies on its top
   * @param entrance the entrance
   * @param surface the block type of the ground it opens in
   * @throws IllegalArgumentException if the entrance does not cover the tile
   */
  public CaveEntranceRenderable(
      int blockX, int blockY, int height, CaveEntrance entrance, BlockType surface) {
    if (!entrance.covers(blockX, blockY)) {
      throw new IllegalArgumentException(
          "entrance " + entrance + " does not cover " + blockX + ", " + blockY);
    }
    this.blockX = blockX;
    this.blockY = blockY;
    this.height = height;
    this.entrance = entrance;
    this.surface = surface;
  }

  /**
   * Returns the entrance this is part of.
   *
   * @return the entrance
   */
  public CaveEntrance getEntrance() {
    return entrance;
  }

  /** Returns {@code null}: the opening is painted, not loaded from an image file. */
  @Override
  public String imageFolder() {
    return null;
  }

  @Override
  public void paintOnto(Graphics2D g, int x, int y, TerrainImages images) {
    g.drawImage(image(), x, y, null);
  }

  @Override
  public Texture getTexture() {
    if (texture == null) {
      texture = new ImageTexture(image());
    }
    return texture;
  }

  /** Returns this tile's square of the entrance's painting. */
  private synchronized BufferedImage image() {
    if (image == null) {
      BufferedImage whole = CaveEntrancePainter.paint(entrance, surface);
      int column = blockX - entrance.minX();
      // The painting has north at the top.
      int rowFromTop = entrance.minY() + entrance.heightBlocks() - 1 - blockY;
      image = whole.getSubimage(column * BLOCK, rowFromTop * BLOCK, BLOCK, BLOCK);
    }
    return image;
  }

  @Override
  public int getX() {
    return TerrainCoordinates.blockToPixel(blockX) + BLOCK / 2;
  }

  @Override
  public int getY() {
    return TerrainCoordinates.blockToPixel(blockY) + height * TileRenderable.STEP_HEIGHT_PX;
  }

  @Override
  public int getWidth() {
    return BLOCK;
  }

  @Override
  public int getHeight() {
    return BLOCK;
  }
}
