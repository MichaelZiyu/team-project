package terrainscene;

import java.awt.image.BufferedImage;

/**
 * A painted chunk, ready to show.
 *
 * @param chunkX the chunk column, in chunks
 * @param chunkY the chunk row, in chunks
 * @param image the painted chunk, possibly taller than the chunk because of raised tiles;
 *     see-through over water when the water is painted separately
 * @param bottomPx the world pixel y of the image's bottom edge, which is the chunk's bottom edge
 * @param water the chunk's water, exactly covering the chunk, to draw underneath everything else;
 *     {@code null} if the water is part of {@code image} or there is none
 */
record ChunkImage(int chunkX, int chunkY, BufferedImage image, int bottomPx, BufferedImage water) {

  /**
   * Creates a painted chunk with no separate water.
   *
   * @param chunkX the chunk column, in chunks
   * @param chunkY the chunk row, in chunks
   * @param image the painted chunk
   * @param bottomPx the world pixel y of the image's bottom edge
   */
  ChunkImage(int chunkX, int chunkY, BufferedImage image, int bottomPx) {
    this(chunkX, chunkY, image, bottomPx, null);
  }

  /**
   * Returns the chunk's water as a chunk image of its own, to draw underneath the terrain.
   *
   * @return the water, or {@code null} if there is none
   */
  ChunkImage waterLayer() {
    return water == null ? null : new ChunkImage(chunkX, chunkY, water, bottomPx);
  }
}
