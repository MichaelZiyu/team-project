
package file;

import game.World;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
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

    //TODO this method should be done using some packages such as jackson or gson
    //which need some changes in pom.xml.
    public static Map<String, Map<String, String>> loadtransition() {
        try (InputStream input = FileManager.class.getResourceAsStream("transitions.json")) {
            if (input == null) {
                throw new RuntimeException("Cannot find file/transitions.json");
            }

            String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            int index = 0;
            Map<String, Map<String, String>> result = new LinkedHashMap<>();

            while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                index++;
            }

            if (index >= json.length() || json.charAt(index++) != '{') {
                throw new IllegalArgumentException("Expected '{' at position " + index);
            }

            while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                index++;
            }

            if (index < json.length() && json.charAt(index) == '}') {
                index++;
                while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                    index++;
                }
                if (index != json.length()) {
                    throw new IllegalArgumentException("Unexpected content after JSON object at position " + index);
                }
                return result;
            }

            while (true) {
                while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                    index++;
                }

                if (index >= json.length() || json.charAt(index++) != '"') {
                    throw new IllegalArgumentException("Expected '\"' at position " + index);
                }

                StringBuilder stateBuilder = new StringBuilder();
                boolean stateClosed = false;

                while (index < json.length()) {
                    char c = json.charAt(index++);

                    if (c == '"') {
                        stateClosed = true;
                        break;
                    }

                    if (c == '\\') {
                        if (index >= json.length()) {
                            throw new IllegalArgumentException("Incomplete escape sequence at position " + index);
                        }

                        char escaped = json.charAt(index++);

                        switch (escaped) {
                            case '"': stateBuilder.append('"'); break;
                            case '\\': stateBuilder.append('\\'); break;
                            case '/': stateBuilder.append('/'); break;
                            case 'b': stateBuilder.append('\b'); break;
                            case 'f': stateBuilder.append('\f'); break;
                            case 'n': stateBuilder.append('\n'); break;
                            case 'r': stateBuilder.append('\r'); break;
                            case 't': stateBuilder.append('\t'); break;
                            case 'u':
                                if (index + 4 > json.length()) {
                                    throw new IllegalArgumentException("Incomplete Unicode escape at position " + index);
                                }
                                String hex = json.substring(index, index + 4);
                                try {
                                    stateBuilder.append((char) Integer.parseInt(hex, 16));
                                } catch (NumberFormatException e) {
                                    throw new IllegalArgumentException("Invalid Unicode escape at position " + index);
                                }
                                index += 4;
                                break;
                            default:
                                throw new IllegalArgumentException("Invalid escape character: " + escaped + " at position " + index);
                        }
                    } else {
                        if (c < 0x20) {
                            throw new IllegalArgumentException("Unescaped control character at position " + index);
                        }
                        stateBuilder.append(c);
                    }
                }

                if (!stateClosed) {
                    throw new IllegalArgumentException("Unterminated string at position " + index);
                }

                String state = stateBuilder.toString();

                while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                    index++;
                }

                if (index >= json.length() || json.charAt(index++) != ':') {
                    throw new IllegalArgumentException("Expected ':' at position " + index);
                }

                while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                    index++;
                }

                if (index >= json.length() || json.charAt(index++) != '{') {
                    throw new IllegalArgumentException("Expected '{' at position " + index);
                }

                Map<String, String> transitions = new LinkedHashMap<>();

                while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                    index++;
                }

                if (index < json.length() && json.charAt(index) == '}') {
                    index++;
                } else {
                    while (true) {
                        while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                            index++;
                        }

                        if (index >= json.length() || json.charAt(index++) != '"') {
                            throw new IllegalArgumentException("Expected '\"' at position " + index);
                        }

                        StringBuilder inputBuilder = new StringBuilder();
                        boolean inputClosed = false;

                        while (index < json.length()) {
                            char c = json.charAt(index++);

                            if (c == '"') {
                                inputClosed = true;
                                break;
                            }

                            if (c == '\\') {
                                if (index >= json.length()) {
                                    throw new IllegalArgumentException("Incomplete escape sequence at position " + index);
                                }

                                char escaped = json.charAt(index++);

                                switch (escaped) {
                                    case '"': inputBuilder.append('"'); break;
                                    case '\\': inputBuilder.append('\\'); break;
                                    case '/': inputBuilder.append('/'); break;
                                    case 'b': inputBuilder.append('\b'); break;
                                    case 'f': inputBuilder.append('\f'); break;
                                    case 'n': inputBuilder.append('\n'); break;
                                    case 'r': inputBuilder.append('\r'); break;
                                    case 't': inputBuilder.append('\t'); break;
                                    case 'u':
                                        if (index + 4 > json.length()) {
                                            throw new IllegalArgumentException("Incomplete Unicode escape at position " + index);
                                        }
                                        String hex = json.substring(index, index + 4);
                                        try {
                                            inputBuilder.append((char) Integer.parseInt(hex, 16));
                                        } catch (NumberFormatException e) {
                                            throw new IllegalArgumentException("Invalid Unicode escape at position " + index);
                                        }
                                        index += 4;
                                        break;
                                    default:
                                        throw new IllegalArgumentException("Invalid escape character: " + escaped + " at position " + index);
                                }
                            } else {
                                if (c < 0x20) {
                                    throw new IllegalArgumentException("Unescaped control character at position " + index);
                                }
                                inputBuilder.append(c);
                            }
                        }

                        if (!inputClosed) {
                            throw new IllegalArgumentException("Unterminated string at position " + index);
                        }

                        String inputKey = inputBuilder.toString();

                        while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                            index++;
                        }

                        if (index >= json.length() || json.charAt(index++) != ':') {
                            throw new IllegalArgumentException("Expected ':' at position " + index);
                        }

                        while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                            index++;
                        }

                        if (index >= json.length() || json.charAt(index++) != '"') {
                            throw new IllegalArgumentException("Expected '\"' at position " + index);
                        }

                        StringBuilder targetBuilder = new StringBuilder();
                        boolean targetClosed = false;

                        while (index < json.length()) {
                            char c = json.charAt(index++);

                            if (c == '"') {
                                targetClosed = true;
                                break;
                            }

                            if (c == '\\') {
                                if (index >= json.length()) {
                                    throw new IllegalArgumentException("Incomplete escape sequence at position " + index);
                                }

                                char escaped = json.charAt(index++);

                                switch (escaped) {
                                    case '"': targetBuilder.append('"'); break;
                                    case '\\': targetBuilder.append('\\'); break;
                                    case '/': targetBuilder.append('/'); break;
                                    case 'b': targetBuilder.append('\b'); break;
                                    case 'f': targetBuilder.append('\f'); break;
                                    case 'n': targetBuilder.append('\n'); break;
                                    case 'r': targetBuilder.append('\r'); break;
                                    case 't': targetBuilder.append('\t'); break;
                                    case 'u':
                                        if (index + 4 > json.length()) {
                                            throw new IllegalArgumentException("Incomplete Unicode escape at position " + index);
                                        }
                                        String hex = json.substring(index, index + 4);
                                        try {
                                            targetBuilder.append((char) Integer.parseInt(hex, 16));
                                        } catch (NumberFormatException e) {
                                            throw new IllegalArgumentException("Invalid Unicode escape at position " + index);
                                        }
                                        index += 4;
                                        break;
                                    default:
                                        throw new IllegalArgumentException("Invalid escape character: " + escaped + " at position " + index);
                                }
                            } else {
                                if (c < 0x20) {
                                    throw new IllegalArgumentException("Unescaped control character at position " + index);
                                }
                                targetBuilder.append(c);
                            }
                        }

                        if (!targetClosed) {
                            throw new IllegalArgumentException("Unterminated string at position " + index);
                        }

                        transitions.put(inputKey, targetBuilder.toString());

                        while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                            index++;
                        }

                        if (index < json.length() && json.charAt(index) == '}') {
                            index++;
                            break;
                        }

                        if (index >= json.length() || json.charAt(index++) != ',') {
                            throw new IllegalArgumentException("Expected ',' at position " + index);
                        }
                    }
                }

                result.put(state, transitions);

                while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                    index++;
                }

                if (index < json.length() && json.charAt(index) == '}') {
                    index++;
                    break;
                }

                if (index >= json.length() || json.charAt(index++) != ',') {
                    throw new IllegalArgumentException("Expected ',' at position " + index);
                }
            }

            while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                index++;
            }

            if (index != json.length()) {
                throw new IllegalArgumentException("Unexpected content after JSON object at position " + index);
            }

            return result;

        } catch (IOException e) {
            throw new RuntimeException("Failed to load file/transitions.json", e);
        }
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
