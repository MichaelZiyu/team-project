package terrainscene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Which grass tiles have a flower growing on them, and which flower.
 *
 * <p>About one grass tile in {@link #ONE_IN} has a flower. Which ones, and which flower each has,
 * comes from the tile's position alone, so the same tiles always flower the same way, whenever and
 * however they are drawn. The flowers to choose from are the image folders under {@link
 * #TEXTURE_FOLDER}; with none there, nothing flowers.
 */
final class Flowers {

  /** Image folder, under the textures root, holding one image folder per flower. */
  static final String TEXTURE_FOLDER = TileRenderable.TEXTURE_FOLDER + "flowers/";

  /** On average, one grass tile in this many has a flower. */
  static final int ONE_IN = 12;

  private static final List<String> NAMES = findFlowers();

  private Flowers() {}

  /**
   * Returns the flowers there are to choose from.
   *
   * @return each flower's name (its image folder's name), in alphabetical order
   */
  static List<String> names() {
    return NAMES;
  }

  /**
   * Returns the flower growing on a grass tile, if any.
   *
   * @param blockX the tile's x coordinate, in blocks
   * @param blockY the tile's y coordinate, in blocks
   * @return the flower's name, or {@code null} if this tile has none
   */
  static String at(int blockX, int blockY) {
    if (NAMES.isEmpty()) {
      return null;
    }
    long hash = mix(blockX * 0x9E3779B97F4A7C15L + blockY * 0xC2B2AE3D27D4EB4FL + 0x51F1E);
    if (Long.remainderUnsigned(hash, ONE_IN) != 0) {
      return null;
    }
    // The rest of the hash picks the flower, so it does not depend on whether the tile flowers.
    return NAMES.get((int) Long.remainderUnsigned(hash / ONE_IN, NAMES.size()));
  }

  /** Scrambles a number so that neighbouring tiles get unrelated results. */
  private static long mix(long z) {
    z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
    z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
    return z ^ (z >>> 31);
  }

  private static List<String> findFlowers() {
    Path folder = TerrainImages.TEXTURES_ROOT.resolve(TEXTURE_FOLDER);
    if (!Files.isDirectory(folder)) {
      return List.of();
    }
    try (Stream<Path> entries = Files.list(folder)) {
      return entries
          .filter(Files::isDirectory)
          .map(entry -> entry.getFileName().toString())
          .sorted()
          .toList();
    } catch (IOException e) {
      return List.of();
    }
  }
}
