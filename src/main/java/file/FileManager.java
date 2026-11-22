
package file;

import game.World;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 *Manages game save data
 *
 * <p>FileManager can be instantiated to manage a specific save.</p>
 * <p>An instance maintains the identity of the current save and provides
 * operations to load and save its associated World.</p>
 */
//TODO looks like the instantiation of FileManager is never useful
public class FileManager {

    public static final Path SAVE_DIRECTORY = Path.of("src", "main", "resources", "saves");
    static final String SAVE_EXTENSION = ".dat";

    private String filename;

    public FileManager(String filename) {
        this.filename = filename;
    }

    public World loadSave() {
        try {
            InputStream stream = FileManager.class.getResourceAsStream("/saves/" + filename + SAVE_EXTENSION);
            if (stream == null) {
                throw new FileNotFoundException(filename);
            }
            return readSavedWorld(stream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load save: " + filename, e);
        }
    }

    static World readSavedWorld(InputStream stream) throws IOException {
        try (stream; ObjectInputStream in = new ObjectInputStream(stream)) {
            if (in.readObject() instanceof World world) {
                return world;
            }
            throw new IOException("Save does not contain a world");
        } catch (ClassNotFoundException e) {
            throw new IOException("Could not read the saved world", e);
        }
    }

    public void saveWorld(World world) {
        try (ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(SAVE_DIRECTORY.resolve(filename + SAVE_EXTENSION).toFile()))) {
            out.writeObject(world);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save world: " + filename, e);
        }
    }

    /**
     * load one texture from provided path
     *
     * @param texturePath a path to the texture relative to resources/textures
     *
     * @return Component object which can be added to JPanel
     */
    public static Component getTexture(String texturePath) {
        String path = "resources/textures/" + texturePath;

        ImageIcon icon = new ImageIcon(path);
        return new JLabel(icon);
    }

    public static Map<JLabel, Integer> getListTexture(String texturePath) {
        Path directory = Path.of(
                "src", "main", "resources", "textures", texturePath
        );

        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException(
                    "Texture directory does not exist: " + directory
            );
        }

        List<Path> imageFiles = new ArrayList<>();

        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString()
                            .matches("(?i).*-[0-9]+\\.(png|jpg|jpeg|gif)$"))
                    .forEach(imageFiles::add);

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read texture directory: " + directory, e
            );
        }

        imageFiles.sort(Comparator.comparing(
                file -> file.getFileName().toString()
        ));

        Map<JLabel, Integer> textures = new LinkedHashMap<>();

        for (Path file : imageFiles) {
            String filename = file.getFileName().toString();

            int duration = Integer.parseInt(
                    filename.substring(
                            filename.lastIndexOf('-') + 1,
                            filename.lastIndexOf('.')
                    )
            );

            ImageIcon icon = new ImageIcon(file.toString());

            if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
                throw new RuntimeException(
                        "Failed to load image: " + file
                );
            }

            textures.put(new JLabel(icon), duration);
        }

        return textures;
    }

}
