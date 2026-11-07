package game;

import game.gameModel.Player;
import system.InputBuffer;
import testpackage.Test;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CommandProcessor {

    public static final Queue<Player.PlayerCommand> commandQueue = new ConcurrentLinkedQueue<>();
    private InputBuffer input;

    // a map from string command to PlayerCommand. Commands controlling moving directions are specially handled by
    // player.movementManager
    private static final Map<String, Player.PlayerCommand> commandMap = new HashMap<>(Map.ofEntries(
            Map.entry("+w", Player.PlayerCommand.MOVE_FORWARD),
            Map.entry("-w", Player.PlayerCommand.CANCEL_FORWARD),
            Map.entry("+s", Player.PlayerCommand.MOVE_BACKWARD),
            Map.entry("-s", Player.PlayerCommand.CANCEL_BACKWARD),
            Map.entry("+a", Player.PlayerCommand.MOVE_LEFT),
            Map.entry("-a", Player.PlayerCommand.CANCEL_LEFT),
            Map.entry("+d", Player.PlayerCommand.MOVE_RIGHT),
            Map.entry("-d", Player.PlayerCommand.CANCEL_RIGHT),

            Map.entry("+shift", Player.PlayerCommand.RUN),
            Map.entry("-shift", Player.PlayerCommand.CANCEL_RUN)
    ));

    public CommandProcessor(InputBuffer input) {
        this.input = input;
    }

    public Iterable<String> getInput() {
        return input.getInput()
                .stream()
                .map(CommandProcessor::translate)
                .filter(Objects::nonNull)
                .toList();
    }

    public static void handle(String command) {
        Player.PlayerCommand c = commandMap.get(command);
        if (c != null) commandQueue.add(c);
    }

    public static String translate(AWTEvent event) {
        if (event instanceof KeyEvent keyEvent) {
            String key = KeyEvent.getKeyText(keyEvent.getKeyCode()).toLowerCase();

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
