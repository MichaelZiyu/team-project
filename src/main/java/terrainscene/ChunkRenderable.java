package terrainscene;

import game.gameModel.Renderable;
import jswing.Texture;
import terrain.TerrainCoordinates;

/** A painted chunk, placed in the world so the game's renderer can draw it like a sprite. */
final class ChunkRenderable implements Renderable {

  private static final long serialVersionUID = 1L;

  private static final int CHUNK_PX =
      TerrainCoordinates.CHUNK_SIZE * TerrainCoordinates.BLOCK_SIZE_PX;

  private final int chunkX;
  private final int chunkY;
  private final int bottomPx;
  private final int heightPx;

  /** Holds a Swing component, so never saved; chunks are repainted from the terrain instead. */
  private final transient Texture texture;

  /**
   * Wraps a painted chunk. Call on the Swing thread, since it creates a Swing component.
   *
   * @param chunk the painted chunk
   */
  ChunkRenderable(ChunkImage chunk) {
    this.chunkX = chunk.chunkX();
    this.chunkY = chunk.chunkY();
    this.bottomPx = chunk.bottomPx();
    this.heightPx = chunk.image().getHeight();
    this.texture = new ImageTexture(chunk.image());
  }

  /**
   * Returns the chunk's column.
   *
   * @return the chunk x coordinate, in chunks
   */
  int getChunkX() {
    return chunkX;
  }

  /**
   * Returns the chunk's row.
   *
   * @return the chunk y coordinate, in chunks
   */
  int getChunkY() {
    return chunkY;
  }

  @Override
  public Texture getTexture() {
    return texture;
  }

  /** Returns the chunk's centre, since the renderer's camera positions sprites by their centre. */
  @Override
  public int getX() {
    return chunkX * CHUNK_PX + CHUNK_PX / 2;
  }

  /** Returns the chunk's bottom edge; raised tiles extend the image upward from here. */
  @Override
  public int getY() {
    return bottomPx;
  }

  @Override
  public int getWidth() {
    return CHUNK_PX;
  }

  @Override
  public int getHeight() {
    return heightPx;
  }
}
