package jswing;

import javax.swing.*;
import game.GameController;

/**
 * initialize and store any panel used in the game
 */
public class PanelManager {

    public static final JPanel startMenuPanel = createStartMenuPanel();
    public static final JPanel settingMenuPanel = createSettingMenuPanel();

    private static JPanel createStartMenuPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JButton startButton = new JButton("Start");
        JButton settingsButton = new JButton("Settings");
        JButton exitButton = new JButton("Exit");

        startButton.addActionListener(e -> {
            // if no save file then generateMenu command
            // if has then loadMenu command
        });

        settingsButton.addActionListener(e -> {
            GameController.transition("Settings");
        });

        exitButton.addActionListener((e -> {
            GameController.transition("Exit");
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
            GameController.transition("Exit");
        });

        panel.add(exitButton);

        return panel;
    }
}
