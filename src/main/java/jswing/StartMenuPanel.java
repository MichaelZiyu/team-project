package jswing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class StartMenuPanel extends BackgroundMenuPanel  {

    // Put image at: src/main/resources/textures/menu/background.png
    private static final String BACKGROUND_PATH =
            "/textures/menu/start-menu-background.png";

    private static final Color FALLBACK_BACKGROUND =
            new Color(24, 29, 40);
    private static final Color CARD_BACKGROUND =
            new Color(34, 41, 55);
    private static final Color BUTTON_BACKGROUND =
            new Color(55, 67, 86);
    private static final Color BUTTON_HOVER =
            new Color(77, 96, 120);
    private static final Color TEXT_COLOR =
            new Color(240, 235, 220);

    public StartMenuPanel(
            Runnable onStart,
            Runnable onSettings,
            Runnable onExit) {

        super(BACKGROUND_PATH, new GridBagLayout());

        setBackground(FALLBACK_BACKGROUND);
        setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel menu = createMenu(onStart, onSettings, onExit);

        // GridBagLayout centres the menu in the window.
        add(menu, new GridBagConstraints());
    }

    private JPanel createMenu(
            Runnable onStart,
            Runnable onSettings,
            Runnable onExit) {

        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(CARD_BACKGROUND);
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(151, 162, 176), 2),
                new EmptyBorder(30, 35, 30, 35)
        ));

        // TODO: Title needed
        JLabel title = new JLabel("JT8-3");
        title.setFont(new Font(Font.SERIF, Font.BOLD, 38));
        title.setForeground(TEXT_COLOR);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        menu.add(title);
        menu.add(Box.createVerticalStrut(32));

        menu.add(createButton("Start Game", onStart));
        menu.add(Box.createVerticalStrut(12));

        menu.add(createButton("Settings", onSettings));
        menu.add(Box.createVerticalStrut(12));

        menu.add(createButton("Exit", onExit));

        return menu;
    }

    private JButton createButton(String text, Runnable action) {
        JButton button = new JButton(text);

        Dimension size = new Dimension(250, 48);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);

        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        button.setForeground(TEXT_COLOR);
        button.setBackground(BUTTON_BACKGROUND);
        button.setOpaque(true);

        // Paint our own background to avoid platform-specific button fills.
        button.setContentAreaFilled(false);

        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(113, 129, 151)),
                new EmptyBorder(10, 18, 10, 18)
        ));
        button.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Keep the focus indicator for keyboard navigation.
        button.setFocusPainted(true);

        button.addActionListener(event -> action.run());

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                button.setBackground(BUTTON_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setBackground(BUTTON_BACKGROUND);
            }
        });

        return button;
    }
}