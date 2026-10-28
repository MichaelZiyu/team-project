package game;

import javax.swing.*;
import java.awt.*;

public class Panels {

    public static final PanelInput input = new PanelInput();

    public static final JPanel startMenuPanel = createStartMenuPanel();

    private static JPanel createStartMenuPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JButton startButton = new JButton("Start");
        JButton settingsButton = new JButton("Settings");

        startButton.addActionListener(e -> input.submit("Start"));
        settingsButton.addActionListener(e -> input.submit("Settings"));

        panel.add(startButton);
        panel.add(settingsButton);

        return panel;
    }
}