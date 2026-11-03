package terrainscene;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

/**
 * Loads terrain images once and shares them, for painting chunk images.
 *
 * <p>Uses the same folder layout as the game's textures: each folder under {@code
 * src/main/resources/textures} holds frames named like {@code 000-1.png}, and terrain uses the
 * first frame. Safe to use from several threads at once.
 */
final class TerrainImages {

  /** Where texture folders live, relative to the working directory; matches the game's loader. */
  static final Path TEXTURES_ROOT = Path.of("src", "main", "resources", "textures");

  private final Path root;
  private final Map<String, BufferedImage> cache = new ConcurrentHashMap<>();

  /** Creates a loader for the game's own texture folders. */
  TerrainImages() {
    this(TEXTURES_ROOT);
  }

  /**
   * Creates a loader for texture folders under a different root.
   *
   * @param root the directory holding the texture folders
   */
  TerrainImages(Path root) {
    this.root = root;
  }

  /**
   * Returns the image in a texture folder, loading it the first time.
   *
   * @param folder a folder relative to the textures root, such as {@code "topdown/terrain/grass"}
   * @return the folder's first frame
   * @throws UncheckedIOException if the folder has no image or it cannot be read
   */
  BufferedImage get(String folder) {
    return cache.computeIfAbsent(folder, this::load);
  }

  private BufferedImage load(String folder) {
    Path directory = root.resolve(folder);
    try (Stream<Path> files = Files.list(directory)) {
      Optional<Path> first =
          files
              .filter(file -> file.getFileName().toString().matches("(?i).*-[0-9]+\\.png"))
              .sorted()
              .findFirst();
      if (first.isEmpty()) {
        throw new IOException("no image frames in " + directory);
      }
      BufferedImage image = ImageIO.read(first.get().toFile());
      if (image == null) {
        throw new IOException("not a readable image: " + first.get());
      }
      return image;
    } catch (IOException e) {
      throw new UncheckedIOException("cannot load terrain image from " + directory, e);
    }
  }
}
