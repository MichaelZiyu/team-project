package terrainscene;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import jswing.Texture;
import terrain.TerrainCoordinates;

/**
 * The surface of one water tile, coloured by its depth (see {@link WaterPainter}).
 *
 * <p>Chunks draw their water as one layer underneath everything else; this piece is the same water
 * one tile at a time, for {@link TerrainScene}, which draws terrain piece by piece. Both come from
 * the same painter, so they look identical.
 */
final class WaterRenderable implements TerrainPiece {

  private static final long serialVersionUID = 1L;

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  private final int blockX;
  private final int blockY;

  /** Painted when the piece is made, and never saved. */
  private final transient BufferedImage image;

  /** Made from the image on first use, and never saved. */
  private transient Texture texture;

  /**
   * Creates the surface of a water tile.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @param painter paints the water
   */
  WaterRenderable(int blockX, int blockY, WaterPainter painter) {
    this.blockX = blockX;
    this.blockY = blockY;
    this.image = painter.paint(blockX, blockY, 1, 1);
  }

  /** Returns {@code null}: water is painted, not loaded from an image file. */
  @Override
  public String imageFolder() {
    return null;
  }

  @Override
  public void paintOnto(Graphics2D g, int x, int y, TerrainImages images) {
    g.drawImage(image, x, y, null);
  }

  @Override
  public Texture getTexture() {
    if (texture == null) {
      texture = new ImageTexture(image);
    }
    return texture;
  }

  @Override
  public int getX() {
    return TerrainCoordinates.blockToPixel(blockX) + BLOCK / 2;
  }

  @Override
  public int getY() {
    return TerrainCoordinates.blockToPixel(blockY);
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
