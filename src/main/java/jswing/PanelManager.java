package jswing;

import javax.swing.*;

import file.FileManager;
import game.GameController;
import states.PanelStateController;
import game.World;

import java.awt.*;

/**
 * initialize and store any panel used in the game
 */
public class PanelManager {

    public static final JPanel startMenuPanel = createStartMenuPanel();
    public static final JPanel settingMenuPanel = createSettingMenuPanel();
    public static final JPanel loadMenuPanel = createLoadMenuPanel();
    public static final JPanel gamePanel = createGamePanel();

    public static final JPanel pausePanel = createPausePanel();

    private static JPanel createStartMenuPanel() {
        return new StartMenuPanel(
                // Start Game
                () -> {
                    if (FileManager.getSaves().length == 0) {
                        PanelStateController.transition("generateMenu");
                    } else {
                        PanelStateController.transition("loadMenu");
                    }
                },

                // Settings
                () -> PanelStateController.transition("settings"),

                // Exit
                () -> PanelStateController.transition("Exit")
        );
    }

    private static JPanel createSettingMenuPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JButton exitButton = new JButton("Exit");

        exitButton.addActionListener(e -> {
            PanelStateController.transition("Exit");
        });

        panel.add(exitButton);

        return panel;
    }

    private static JPanel createLoadMenuPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JButton exitButton = new JButton("Exit");
        JComboBox<String> list = new JComboBox<>(FileManager.getSaves());
        list.setMaximumSize(new Dimension(200, 30));
        list.setPreferredSize(new Dimension(200, 30));
        list.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton loadButton = new JButton("Load");

        loadButton.addActionListener(e -> { // should make the game run
            String selected = (String) list.getSelectedItem();
            FileManager file = new FileManager(selected);
            World world = file.loadSave();
            GameController.initializeGame(world, file);
            PanelStateController.transition("load");
        });

        exitButton.addActionListener(e -> {
            PanelStateController.transition("Exit");
        });

        panel.add(list);
        panel.add(loadButton);
        panel.add(exitButton);

        return panel;
    }

    private static JPanel createGamePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(null);
        return panel;
    }

    private static JPanel createPausePanel() {
        JPanel panel = new JPanel();

        JButton returnButton = new JButton("return the game");

        returnButton.addActionListener(e -> {
            PanelStateController.transition("return");
        });

        panel.add(returnButton);
        return panel;
    }
}
