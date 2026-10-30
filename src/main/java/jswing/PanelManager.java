package jswing;

import javax.swing.*;

import file.FileManager;
import game.GameController;
import states.PanelStateController;
import game.World;

/**
 * initialize and store any panel used in the game
 */
public class PanelManager {

    public static final JPanel startMenuPanel = createStartMenuPanel();
    public static final JPanel settingMenuPanel = createSettingMenuPanel();
    public static final JPanel loadMenuPanel = createLoadMenuPanel();

    public static final JPanel gamePanel = createGamePanel();

    private static JPanel createStartMenuPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JButton startButton = new JButton("Start");
        JButton settingsButton = new JButton("Settings");
        JButton exitButton = new JButton("Exit");

        startButton.addActionListener(e -> {
            if (FileManager.getSaves().length == 0) PanelStateController.transition("generateMenu");
            else PanelStateController.transition("loadMenu");
        });

        settingsButton.addActionListener(e -> {
            PanelStateController.transition("Settings");
        });

        exitButton.addActionListener((e -> {
            PanelStateController.transition("Exit");
        }));

        panel.add(startButton);
        panel.add(settingsButton);
        panel.add(exitButton);

        return panel;
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
        JButton loadButton = new JButton("Load");

        loadButton.addActionListener(e -> { // should make the game run
            PanelStateController.transition("gameState");
            String selected = (String) list.getSelectedItem();
            FileManager file = new FileManager(selected);
            World world = file.loadSave();
            GameController.initializeGame(world, file);
        });

        exitButton.addActionListener(e -> {
            PanelStateController.transition("Exit");
        });

        panel.add(list);
        panel.add(exitButton);
        panel.add(loadButton);

        return panel;
    }

    private static JPanel createGamePanel() {
        JPanel panel = new JPanel();
        return panel;
    }
}
