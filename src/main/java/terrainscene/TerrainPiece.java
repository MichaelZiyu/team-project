package terrainscene;

import game.gameModel.Renderable;
import java.awt.Graphics2D;

/**
 * One drawable piece of terrain (a tile top, face step, shadow, rim or occlusion strip) that also
 * says which image it shows.
 *
 * <p>Knowing how to paint itself lets {@link ChunkPainter} put a piece straight into a chunk image
 * without creating a Swing component for each one. Pieces load their {@link
 * Renderable#getTexture() texture} only when the renderer first asks for it.
 */
public interface TerrainPiece extends Renderable {

  /**
   * Returns the folder of this piece's image, relative to the textures root.
   *
   * @return a folder such as {@code "topdown/terrain/grass"}, or {@code null} for a piece that is
   *     drawn as a shape instead of from an image
   */
  String imageFolder();

  /**
   * Paints this piece into an image, such as a chunk image.
   *
   * <p>By default this draws the piece's image at its size. Pieces drawn as shapes override it.
   *
   * @param g the graphics to paint with
   * @param x where the piece's left edge goes, in image pixels
   * @param y where the piece's top edge goes, in image pixels (y down)
   * @param images where piece images are loaded from
   */
  default void paintOnto(Graphics2D g, int x, int y, TerrainImages images) {
    g.drawImage(images.get(imageFolder()), x, y, getWidth(), getHeight(), null);
  }
}
