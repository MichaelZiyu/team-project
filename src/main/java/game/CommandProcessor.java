package game;

import java.util.HashMap;
import java.util.Map;

public class CommandProcessor {

    private static final Map<Character, Integer> keyMap = new HashMap<>(Map.of(
            'w', 0,
            'a', 0,
            's', 0,
            'd', 0
    ));
    private static final Map<Character, Character> pairs = Map.of(
            'w', 's',
            's', 'w',
            'a', 'd',
            'd', 'a'
    );

    //ws, ad
    public static final char[] currentInput = {'\0', '\0'};

    private static int pairId(char key) {
        if (key == 'w' || key == 's') return 0;
        return 1;
    }

    public static void handle(String command) {
        boolean press = command.charAt(0) == '+';
        char key = command.charAt(1);

        if (press) {
            keyMap.put(key, 1);
            currentInput[pairId(key)] = key;
        } else {
            keyMap.put(key, 0);
            if (keyMap.get(pairs.get(key)) == 1) currentInput[pairId(key)] = pairs.get(key);
            else currentInput[pairId(key)] = '\0';
        }
    }
}
