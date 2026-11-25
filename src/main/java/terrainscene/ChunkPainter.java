package terrainscene;

import game.gameModel.Renderable;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import terrain.Terrain;
import terrain.TerrainCoordinates;
import terrain.Trees;

/**
 * Paints a whole chunk of terrain (tiles, faces, lighting, rims, shadows and occlusion) into one
 * image, or any other rectangle of tiles.
 *
 * <p>A chunk's water is painted into an image of its own, to be drawn underneath the rest, so it
 * can later move without repainting the terrain; the terrain image is see-through over water,
 * apart from the shadows and shading lying on it.
 *
 * <p>Drawing one image per chunk instead of one Swing component per piece is what keeps the game
 * fast: about a dozen chunk images are on screen instead of hundreds of pieces. The image is
 * taller than the chunk, because raised tiles are lifted up past its top edge; the space above
 * lower tiles is transparent. Pieces come from {@link TilePieces}, so a painted chunk looks the
 * same as the same tiles drawn piece by piece. Safe to call from several threads at once.
 */
final class ChunkPainter implements ChunkedTerrainScene.ChunkSource {

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;
  private static final int SIZE = TerrainCoordinates.CHUNK_SIZE;

  private final Terrain terrain;
  private final TilePieces tilePieces;
  private final WaterPainter water;
  private final TerrainImages images;

  /**
   * Creates a painter.
   *
   * @param terrain the terrain to paint
   * @param images where piece images are loaded from
   */
  ChunkPainter(Terrain terrain, TerrainImages images) {
    this(terrain, null, images);
  }

  /**
   * Creates a painter whose terrain shows the shadows of the trees growing on it.
   *
   * @param terrain the terrain to paint
   * @param trees the trees growing on it; {@code null} for none
   * @param images where piece images are loaded from
   */
  ChunkPainter(Terrain terrain, Trees trees, TerrainImages images) {
    this.terrain = terrain;
    this.tilePieces = new TilePieces(terrain, new PlaceholderPieces(), false, trees);
    this.water = new WaterPainter(terrain);
    this.images = images;
  }

  @Override
  public ChunkImage paint(int chunkX, int chunkY, TerrainLight sun) {
    int firstX = chunkX * SIZE;
    int firstY = chunkY * SIZE;
    AreaImage area = paintTerrain(firstX, firstY, SIZE, SIZE, sun);
    BufferedImage waterImage = water.paint(firstX, firstY, SIZE, SIZE);
    return new ChunkImage(chunkX, chunkY, area.image(), area.bottomPx(), waterImage);
  }

  /**
   * Paints a rectangle of tiles into one image, with its water as a separate image to draw
   * underneath, the same way as a chunk. The water is darkened as much as the light darkens
   * terrace faces, so under lightning it is as dark as the sides of the blocks; anything drawn
   * between the two (such as the ripples) shows through the image over the water.
   *
   * @param firstX the left column of tiles, in blocks
   * @param firstY the bottom row of tiles, in blocks
   * @param widthBlocks how many columns to paint
   * @param heightBlocks how many rows to paint
   * @param sun the light to paint them in
   * @return the image, taller than the rectangle by the height of its tallest tile, with where it
   *     goes in the world and its water
   */
  AreaImage paintArea(
      int firstX, int firstY, int widthBlocks, int heightBlocks, TerrainLight sun) {
    AreaImage area = paintTerrain(firstX, firstY, widthBlocks, heightBlocks, sun);
    BufferedImage waterImage = water.paint(firstX, firstY, widthBlocks, heightBlocks);
    if (waterImage != null && sun.faceDarkening() > 0) {
      Graphics2D g = waterImage.createGraphics();
      try {
        // Only over the water itself, leaving land see-through.
        g.setComposite(AlphaComposite.SrcAtop);
        Color dark = TintRenderable.DARK_COLOUR;
        g.setColor(
            new Color(
                dark.getRed(),
                dark.getGreen(),
                dark.getBlue(),
                (int) Math.round(sun.faceDarkening() * 255)));
        g.fillRect(0, 0, waterImage.getWidth(), waterImage.getHeight());
      } finally {
        g.dispose();
      }
    }
    return new AreaImage(area.image(), area.leftPx(), area.bottomPx(), waterImage);
  }

  /** Paints a rectangle of tiles, leaving the water see-through. */
  private AreaImage paintTerrain(
      int firstX, int firstY, int widthBlocks, int heightBlocks, TerrainLight sun) {
    int maxHeight = 0;
    for (int localX = 0; localX < widthBlocks; localX++) {
      for (int localY = 0; localY < heightBlocks; localY++) {
        maxHeight = Math.max(maxHeight, terrain.heightAt(firstX + localX, firstY + localY));
      }
    }
    int leftPx = TerrainCoordinates.blockToPixel(firstX);
    int bottomPx = TerrainCoordinates.blockToPixel(firstY);
    int widthPx = widthBlocks * BLOCK;
    int heightPx = heightBlocks * BLOCK + maxHeight * TileRenderable.STEP_HEIGHT_PX;
    int topPx = bottomPx + heightPx;

    BufferedImage image = new BufferedImage(widthPx, heightPx, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    try {
      // Furthest row (highest y) first, so nearer rows are painted over the ones behind them.
      for (int localY = heightBlocks - 1; localY >= 0; localY--) {
        for (int localX = 0; localX < widthBlocks; localX++) {
          paintTile(g, firstX + localX, firstY + localY, leftPx, topPx, sun);
        }
      }
    } finally {
      g.dispose();
    }
    return new AreaImage(image, leftPx, bottomPx);
  }

  /** Paints one tile's pieces, bottom-most first (the reverse of the renderer's order). */
  private void paintTile(
      Graphics2D g, int blockX, int blockY, int leftPx, int topPx, TerrainLight sun) {
    List<Renderable> pieces = tilePieces.build(blockX, blockY, sun);
    for (int i = pieces.size() - 1; i >= 0; i--) {
      TerrainPiece piece = (TerrainPiece) pieces.get(i);
      // Same placement rule as the game's camera: x is the centre, y is the bottom edge.
      int x = piece.getX() - piece.getWidth() / 2 - leftPx;
      int y = topPx - (piece.getY() + piece.getHeight());
      piece.paintOnto(g, x, y, images);
    }
  }
}
