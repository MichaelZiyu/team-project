package jswing;

import game.GameSettings;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Control reference and session-wide Shift behavior. */
public final class SettingsPanel extends JPanel {
    private final JRadioButton hold = new JRadioButton("Hold Shift to run");
    private final JRadioButton toggle = new JRadioButton("Press Shift to switch between run and walk");

    public SettingsPanel(Runnable onBack) {
        setLayout(new BorderLayout(0, 18));
        setBackground(new Color(24, 29, 40));
        setBorder(new EmptyBorder(24, 32, 24, 32));

        JLabel heading = new JLabel("Settings");
        heading.setForeground(new Color(240, 235, 220));
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        add(heading, BorderLayout.NORTH);

        JTextArea controls = new JTextArea(
                "PLAYER CONTROLS\n\n"
                + "W          Move up / forward\n"
                + "S          Move down / backward\n"
                + "A          Move left\n"
                + "D          Move right\n"
                + "Shift      Run (3x walking speed)\n"
                + "Escape     Leave gameplay and return to load menu\n\n"
                + "Release a movement key to stop that direction.\n"
                + "In hold mode, release Shift to return to walking.\n"
                + "In toggle mode, press and release Shift once to run,\n"
                + "then press it again to walk.\n\n"
                + "Inventory, attack and interact have no key bindings yet.\n"
                + "Left/right mouse events are captured but have no action yet.");
        controls.setEditable(false);
        controls.setLineWrap(true);
        controls.setWrapStyleWord(true);
        controls.setCaretPosition(0);
        controls.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        controls.setBackground(new Color(34, 41, 55));
        controls.setForeground(new Color(240, 235, 220));
        controls.setBorder(new EmptyBorder(12, 14, 12, 14));
        JScrollPane scroll = new JScrollPane(controls);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(113, 129, 151)));

        ButtonGroup modes = new ButtonGroup();
        modes.add(hold);
        modes.add(toggle);
        hold.setOpaque(false);
        toggle.setOpaque(false);
        hold.setForeground(new Color(240, 235, 220));
        toggle.setForeground(new Color(240, 235, 220));
        hold.addActionListener(event -> GameSettings.setRunMode(GameSettings.RunMode.HOLD));
        toggle.addActionListener(event -> GameSettings.setRunMode(GameSettings.RunMode.TOGGLE));
        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.setOpaque(false);
        options.add(hold);
        options.add(toggle);
        JLabel note = new JLabel("Changes apply for this session. Switching mode resets running.");
        note.setForeground(new Color(180, 192, 205));
        note.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        options.add(Box.createVerticalStrut(8));
        options.add(note);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);
        content.add(scroll, BorderLayout.CENTER);
        content.add(options, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);

        JButton back = new JButton("Back");
        back.addActionListener(event -> onBack.run());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        footer.add(back);
        add(footer, BorderLayout.SOUTH);
        refreshMode();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        refreshMode();
    }

    private void refreshMode() {
        hold.setSelected(GameSettings.getRunMode() == GameSettings.RunMode.HOLD);
        toggle.setSelected(GameSettings.getRunMode() == GameSettings.RunMode.TOGGLE);
    }
}
