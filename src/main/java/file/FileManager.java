
package file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class FileManager {

    //this method should be done using some packages such as jackson or gson
    //which need some changes in pom.xml.
    //TODO Jimmy should decide how to implement this method.
    public static Map<String, Map<String, String>> loadtransition() {
        try (InputStream input = FileManager.class.getResourceAsStream("transitions.json")) {
            if (input == null) {
                throw new RuntimeException("Cannot find file/transitions.json");
            }

            String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return new TransitionParser(json).parse();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load file/transitions.json", e);
        }
    }

    private static class TransitionParser {

        private final String json;
        private int index = 0;

        private TransitionParser(String json) {
            this.json = json;
        }

        private Map<String, Map<String, String>> parse() {
            Map<String, Map<String, String>> result = new LinkedHashMap<>();

            skipWhitespace();
            expect('{');
            skipWhitespace();

            if (consume('}')) {
                ensureEnd();
                return result;
            }

            while (true) {
                String state = parseString();

                skipWhitespace();
                expect(':');
                skipWhitespace();

                Map<String, String> transitions = parseInnerObject();
                result.put(state, transitions);

                skipWhitespace();

                if (consume('}')) {
                    break;
                }

                expect(',');
                skipWhitespace();
            }

            ensureEnd();
            return result;
        }

        private Map<String, String> parseInnerObject() {
            Map<String, String> result = new LinkedHashMap<>();

            expect('{');
            skipWhitespace();

            if (consume('}')) {
                return result;
            }

            while (true) {
                String input = parseString();

                skipWhitespace();
                expect(':');
                skipWhitespace();

                String targetState = parseString();
                result.put(input, targetState);

                skipWhitespace();

                if (consume('}')) {
                    break;
                }

                expect(',');
                skipWhitespace();
            }

            return result;
        }

        private String parseString() {
            expect('"');

            StringBuilder result = new StringBuilder();

            while (index < json.length()) {
                char c = json.charAt(index++);

                if (c == '"') {
                    return result.toString();
                }

                if (c == '\\') {
                    if (index >= json.length()) {
                        throw error("Incomplete escape sequence");
                    }

                    char escaped = json.charAt(index++);

                    switch (escaped) {
                        case '"':  result.append('"');  break;
                        case '\\': result.append('\\'); break;
                        case '/':  result.append('/');  break;
                        case 'b':  result.append('\b'); break;
                        case 'f':  result.append('\f'); break;
                        case 'n':  result.append('\n'); break;
                        case 'r':  result.append('\r'); break;
                        case 't':  result.append('\t'); break;

                        case 'u':
                            if (index + 4 > json.length()) {
                                throw error("Incomplete Unicode escape");
                            }

                            String hex = json.substring(index, index + 4);

                            try {
                                result.append((char) Integer.parseInt(hex, 16));
                            } catch (NumberFormatException e) {
                                throw error("Invalid Unicode escape");
                            }

                            index += 4;
                            break;

                        default:
                            throw error("Invalid escape character: " + escaped);
                    }
                } else {
                    if (c < 0x20) {
                        throw error("Unescaped control character");
                    }

                    result.append(c);
                }
            }

            throw error("Unterminated string");
        }

        private void skipWhitespace() {
            while (index < json.length()
                    && Character.isWhitespace(json.charAt(index))) {
                index++;
            }
        }

        private void expect(char expected) {
            if (!consume(expected)) {
                throw error("Expected '" + expected + "'");
            }
        }

        private boolean consume(char expected) {
            if (index < json.length() && json.charAt(index) == expected) {
                index++;
                return true;
            }

            return false;
        }

        private void ensureEnd() {
            skipWhitespace();

            if (index != json.length()) {
                throw error("Unexpected content after JSON object");
            }
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(
                    message + " at position " + index
            );
        }
    }
}
