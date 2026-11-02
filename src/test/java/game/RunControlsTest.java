package game;

import game.gameModel.Player;
import jswing.SettingsPanel;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/** Dependency-free regression test: execute game.RunControlsTest from project root. */
public final class RunControlsTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        check(GameSettings.getRunMode() == GameSettings.RunMode.HOLD, "Hold is the default");
        World world = new World();
        Player player = world.getPlayer();
        CommandProcessor.reset();
        player.handleCommand();
        world.takeCommand("+d");
        checkDistance(player, 2, "Walk speed");
        world.takeCommand("+shift");
        checkDistance(player, 6, "Hold Shift runs");
        world.takeCommand("+shift");
        checkDistance(player, 6, "Repeated key press still runs");
        world.takeCommand("-shift");
        checkDistance(player, 2, "Release Shift walks");

        GameSettings.setRunMode(GameSettings.RunMode.TOGGLE);
        world.tick();
        checkDistance(player, 2, "Mode switch resets running");
        world.takeCommand("+shift");
        checkDistance(player, 6, "First press toggles run on");
        for (int i = 0; i < 20; i++) { world.takeCommand("+shift"); }
        checkDistance(player, 6, "Auto-repeat cannot retoggle");
        world.takeCommand("-shift");
        checkDistance(player, 6, "Release preserves toggle run");
        world.takeCommand("+shift");
        checkDistance(player, 2, "Second press toggles run off");
        world.takeCommand("-shift");
        world.takeCommand("+shift");
        world.takeCommand("-shift");
        GameSettings.setRunMode(GameSettings.RunMode.HOLD);
        world.tick();
        checkDistance(player, 2, "Changing to hold resets toggled run");

        world.takeCommand("+shift");
        CommandProcessor.reset();
        player.handleCommand();
        check(player.velocity[0] == 0 && player.velocity[1] == 0, "Focus/session reset stops movement");
        world.takeCommand("+d");
        checkDistance(player, 2, "Focus/session reset stops running");
        world.takeCommand("+a");
        check(player.velocity[0] == -120, "Latest opposing direction wins");
        world.takeCommand("-a");
        check(player.velocity[0] == 120, "Releasing opposite restores held key");
        world.takeCommand("-d");
        check(player.velocity[0] == 0, "Release stops the axis");
        world.takeCommand("+w");
        check(player.velocity[1] == 120, "W moves upward");
        world.takeCommand("-w");
        world.takeCommand("+s");
        check(player.velocity[1] == -120, "S moves downward");
        world.takeCommand("+shift");
        world.takeCommand("+esc");
        check(world.pause && player.velocity[0] == 0 && player.velocity[1] == 0,
                "Escape stops without falling through into movement");

        JPanel source = new JPanel();
        for (int code : new int[]{KeyEvent.VK_W, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D,
                KeyEvent.VK_SHIFT, KeyEvent.VK_ESCAPE}) {
            String key = code == KeyEvent.VK_SHIFT ? "shift" : code == KeyEvent.VK_ESCAPE ? "esc"
                    : String.valueOf((char) code).toLowerCase(java.util.Locale.ROOT);
            for (int id : new int[]{KeyEvent.KEY_PRESSED, KeyEvent.KEY_RELEASED}) {
                KeyEvent event = new KeyEvent(source, id, 0, 0, code, KeyEvent.CHAR_UNDEFINED);
                check(CommandProcessor.translate(event).equals((id == KeyEvent.KEY_PRESSED ? "+" : "-") + key),
                        "Stable press/release translation for " + key);
            }
        }
        check(CommandProcessor.translate(new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, 0,
                KeyEvent.VK_E, 'e')) == null, "Unbound keys are ignored");

        SwingUtilities.invokeAndWait(() -> {
            SettingsPanel panel = new SettingsPanel(() -> { });
            JRadioButton hold = find(panel, JRadioButton.class, "Hold Shift to run");
            JRadioButton toggle = find(panel, JRadioButton.class, "Press Shift to switch between run and walk");
            check(hold.isSelected() && !toggle.isSelected(), "UI reflects hold mode");
            toggle.doClick();
            check(GameSettings.getRunMode() == GameSettings.RunMode.TOGGLE && !hold.isSelected(),
                    "Toggle option changes game setting");
            hold.doClick();
            check(GameSettings.getRunMode() == GameSettings.RunMode.HOLD && !toggle.isSelected(),
                    "Hold option changes game setting");
            JTextArea controls = find(panel, JTextArea.class, null);
            check(!controls.isEditable() && controls.getText().contains("Escape"), "Read-only controls reference");
            panel.setSize(800, 560);
            layout(panel);
            BufferedImage image = new BufferedImage(800, 560, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            panel.printAll(g);
            g.dispose();
            try {
                Files.createDirectories(Path.of("build"));
                ImageIO.write(image, "png", Path.of("build/settings-preview.png").toFile());
            } catch (Exception exception) { throw new RuntimeException(exception); }
        });
        System.out.println("PASS: " + checks + " run/control/settings checks");
    }

    private static void checkDistance(Player player, double expected, String label) {
        double before = player.coordinate[0];
        player.tick();
        check(Math.abs(player.coordinate[0] - before - expected) < 0.00001, label);
    }
    private static void check(boolean value, String message) {
        if (!value) { throw new AssertionError(message); }
        checks++;
    }
    private static <T extends Component> T find(Container root, Class<T> type, String text) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child) && (text == null
                    || ((AbstractButton) child).getText().equals(text))) { return type.cast(child); }
            if (child instanceof Container) {
                T found = find((Container) child, type, text);
                if (found != null) { return found; }
            }
        }
        return null;
    }
    private static void layout(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) {
            if (child instanceof Container) { layout((Container) child); }
        }
    }
}
