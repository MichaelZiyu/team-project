
package file;

import game.World;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *Manages game save data
 *
 * <p>FileManager provides static methods for accessing existing saves,
 * and can also be instantiated to manage a specific save.</p>
 * <p>An instance maintains the identity of the current save and provides
 * operations to load and save its associated World.</p>
 */
public class FileManager {

    private String filename;

    public FileManager(String filename) {
        this.filename = filename;
    }

    //this method should be done using some packages such as jackson or gson
    //which need some changes in pom.xml.
    //*TODO Jimmy should decide how to implement this method.*
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

    public static String[] getSaves() {
        List<String> saves = new ArrayList<>();

        try {
            Path savesPath = Path.of(
                    FileManager.class.getResource("/").toURI()
            ).resolve("saves");

            if (!Files.exists(savesPath) || !Files.isDirectory(savesPath)) {
                return new String[0];
            }

            try (var files = Files.list(savesPath)) {
                files.filter(Files::isRegularFile)
                        .forEach(path -> {
                            String fileName = path.getFileName().toString();
                            int dotIndex = fileName.lastIndexOf('.');
                            saves.add(dotIndex > 0
                                    ? fileName.substring(0, dotIndex)
                                    : fileName);
                        });
            }

            return saves.toArray(new String[0]);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load saves", e);
        }
    }

    public World loadSave() {
        try (ObjectInputStream in = new ObjectInputStream(
                FileManager.class.getResourceAsStream("/saves/" + filename + ".dat"))) {
            return (World) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to load save: " + filename, e);
        }
    }

    public void saveWorld(World world) {
        try (ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream("src/main/resources/saves/" + filename + ".dat"))) {
            out.writeObject(world);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save world: " + filename, e);
        }
    }
}
