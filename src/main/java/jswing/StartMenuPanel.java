package jswing;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class StartMenuPanel extends JPanel {

    // TODO: BACKGROUND IMAGE
    // Put your image at:
    // src/main/resources/textures/menu/background.png
    private static final String BACKGROUND_PATH =
            "/textures/menu/background.png";

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

    private final BufferedImage background;

    public StartMenuPanel(
            Runnable onStart,
            Runnable onSettings,
            Runnable onExit) {

        background = loadBackground();

        setBackground(FALLBACK_BACKGROUND);
        setLayout(new GridBagLayout());
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
        JLabel title = new JLabel("placeholder");
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

    private BufferedImage loadBackground() {
        URL resource = StartMenuPanel.class.getResource(BACKGROUND_PATH);

        // The image is optional.
        if (resource == null) {
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(resource);
            if (image == null) {
                throw new IllegalStateException(
                        "Unsupported background image: " + BACKGROUND_PATH);
            }
            return image;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Cannot load background: " + BACKGROUND_PATH,
                    exception
            );
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        if (background == null) {
            return;
        }

        Graphics2D g = (Graphics2D) graphics.create();

        try {
            // Keeps pixel-art edges sharp.
            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
            );

            // Fill the panel while preserving the image's aspect ratio.
            // Excess image area is cropped at the edges.
            double scale = Math.max(
                    (double) getWidth() / background.getWidth(),
                    (double) getHeight() / background.getHeight()
            );

            int width = (int) Math.ceil(background.getWidth() * scale);
            int height = (int) Math.ceil(background.getHeight() * scale);
            int x = (getWidth() - width) / 2;
            int y = (getHeight() - height) / 2;

            g.drawImage(background, x, y, width, height, null);

            // Darken the background slightly for readability.
            g.setColor(new Color(0, 0, 0, 65));
            g.fillRect(0, 0, getWidth(), getHeight());
        } finally {
            g.dispose();
        }
    }
}