package terrainscene;

import java.awt.image.BufferedImage;

/**
 * A painted chunk, ready to show.
 *
 * @param chunkX the chunk column, in chunks
 * @param chunkY the chunk row, in chunks
 * @param image the painted chunk, possibly taller than the chunk because of raised tiles
 * @param bottomPx the world pixel y of the image's bottom edge, which is the chunk's bottom edge
 */
record ChunkImage(int chunkX, int chunkY, BufferedImage image, int bottomPx) {}
