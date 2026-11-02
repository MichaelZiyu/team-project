package game;

import game.gameModel.Player;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CommandProcessor {

    public static final Queue<Player.PlayerCommand> commandQueue = new ConcurrentLinkedQueue<>();
    private static final Map<String, Player.PlayerCommand> commandMap = new HashMap<>(Map.ofEntries(
            Map.entry("w", Player.PlayerCommand.MOVE_FORWARD),
            Map.entry("s", Player.PlayerCommand.MOVE_BACKWARD),
            Map.entry("a", Player.PlayerCommand.MOVE_LEFT),
            Map.entry("d", Player.PlayerCommand.MOVE_RIGHT)
    ));

    private static final Map<Character, Integer> keyMap = new HashMap<>(Map.of(
            'w', 0,
            'a', 0,
            's', 0,
            'd', 0
    ));

    private static final RunInput runInput = new RunInput();

    private static final char[] inputs = {'\0', '\0'};

    private static int pairId(char key) {
        if (key == 'w' || key == 's') return 0;
        return 1;
    }

    public static void handleDirection(String command) {
        boolean press = command.charAt(0) == '+';
        char key = command.charAt(1);
        Map<Character, Character> pairs = Map.of(
                'w', 's',
                's', 'w',
                'a', 'd',
                'd', 'a'
        );

        if (press) {
            keyMap.put(key, 1);
            inputs[pairId(key)] = key;
        } else {
            keyMap.put(key, 0);
            if (keyMap.get(pairs.get(key)) == 1) inputs[pairId(key)] = pairs.get(key);
            else inputs[pairId(key)] = '\0';
        }

        if (inputs[0] == '\0') commandQueue.add(Player.PlayerCommand.CANCEL_WS);
        else commandQueue.add(commandMap.get("" + inputs[0]));
        if (inputs[1] == '\0') commandQueue.add(Player.PlayerCommand.CANCEL_AD);
        else commandQueue.add(commandMap.get("" + inputs[1]));
    }

    public static void handle(String command) {
        synchronizeRunMode();
        if ("+shift".equals(command) || "-shift".equals(command)) {
            boolean before = runInput.isRunning();
            if ("+shift".equals(command)) { runInput.press(); }
            else { runInput.release(); }
            if (before != runInput.isRunning()) {
                commandQueue.add(runInput.isRunning()
                        ? Player.PlayerCommand.RUN : Player.PlayerCommand.CANCEL_RUN);
            }
            return;
        }
        Player.PlayerCommand c = commandMap.get(command);
        if (c != null) commandQueue.add(c);
    }

    /** Called on the game thread; a settings change starts in walking mode. */
    public static void synchronizeRunMode() {
        if (runInput.setMode(GameSettings.getRunMode())) {
            commandQueue.add(Player.PlayerCommand.CANCEL_RUN);
        }
    }

    /** Clear held/toggled inputs when loading or leaving gameplay. */
    public static void reset() {
        commandQueue.clear();
        keyMap.replaceAll((key, value) -> 0);
        inputs[0] = inputs[1] = '\0';
        runInput.reset();
        runInput.setMode(GameSettings.getRunMode());
        commandQueue.add(Player.PlayerCommand.CANCEL_WS);
        commandQueue.add(Player.PlayerCommand.CANCEL_AD);
        commandQueue.add(Player.PlayerCommand.CANCEL_RUN);
    }


    public static String translate(AWTEvent event) {
        if (event instanceof KeyEvent keyEvent) {
            // Stable command names regardless of OS display language.
            String key;
            switch (keyEvent.getKeyCode()) {
                case KeyEvent.VK_W: key = "w"; break;
                case KeyEvent.VK_A: key = "a"; break;
                case KeyEvent.VK_S: key = "s"; break;
                case KeyEvent.VK_D: key = "d"; break;
                case KeyEvent.VK_SHIFT: key = "shift"; break;
                case KeyEvent.VK_ESCAPE: key = "esc"; break;
                default: return null;
            }

            if (keyEvent.getID() == KeyEvent.KEY_PRESSED) {
                return "+" + key;
            } else if (keyEvent.getID() == KeyEvent.KEY_RELEASED) {
                return "-" + key;
            }
        }

        else if (event instanceof MouseEvent mouseEvent) {
            int button = mouseEvent.getButton();
            int id = mouseEvent.getID();

            String key = switch (button) {
                case MouseEvent.BUTTON1 -> "left";
                case MouseEvent.BUTTON3 -> "right";
                default -> null;
            };

            if (key == null) return null;

            if (id == MouseEvent.MOUSE_PRESSED) {
                return "+" + key;
            } else if (id == MouseEvent.MOUSE_RELEASED) {
                return "-" + key;
            }
        }

        return null;
    }
}
