package terrainscene;

import game.gameModel.Renderable;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import terrain.Terrain;
import terrain.TerrainCoordinates;

/**
 * Paints a whole chunk of terrain (tiles, faces, lighting, rims, shadows and occlusion) into one
 * image.
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
  private static final int CHUNK_PX = SIZE * BLOCK;

  private final Terrain terrain;
  private final TilePieces tilePieces;
  private final TerrainImages images;

  /**
   * Creates a painter.
   *
   * @param terrain the terrain to paint
   * @param images where piece images are loaded from
   */
  ChunkPainter(Terrain terrain, TerrainImages images) {
    this.terrain = terrain;
    this.tilePieces = new TilePieces(terrain, new PlaceholderPieces());
    this.images = images;
  }

  @Override
  public ChunkImage paint(int chunkX, int chunkY, ShadowCast sun) {
    int firstX = chunkX * SIZE;
    int firstY = chunkY * SIZE;
    int maxHeight = 0;
    for (int localX = 0; localX < SIZE; localX++) {
      for (int localY = 0; localY < SIZE; localY++) {
        maxHeight = Math.max(maxHeight, terrain.heightAt(firstX + localX, firstY + localY));
      }
    }
    int leftPx = TerrainCoordinates.blockToPixel(firstX);
    int bottomPx = TerrainCoordinates.blockToPixel(firstY);
    int heightPx = CHUNK_PX + maxHeight * TileRenderable.STEP_HEIGHT_PX;
    int topPx = bottomPx + heightPx;

    BufferedImage image = new BufferedImage(CHUNK_PX, heightPx, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    try {
      // Furthest row (highest y) first, so nearer rows are painted over the ones behind them.
      for (int localY = SIZE - 1; localY >= 0; localY--) {
        for (int localX = 0; localX < SIZE; localX++) {
          paintTile(g, firstX + localX, firstY + localY, leftPx, topPx, sun);
        }
      }
    } finally {
      g.dispose();
    }
    return new ChunkImage(chunkX, chunkY, image, bottomPx);
  }

  /** Paints one tile's pieces, bottom-most first (the reverse of the renderer's order). */
  private void paintTile(
      Graphics2D g, int blockX, int blockY, int leftPx, int topPx, ShadowCast sun) {
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
