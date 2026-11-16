package jswing;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/** A menu view with an optional classpath background image. */
public abstract class BackgroundMenuPanel extends JPanel {

    private final BufferedImage background;

    protected BackgroundMenuPanel(String backgroundPath, LayoutManager layout) {
        super(layout);
        background = loadBackground(backgroundPath);
    }

    private static BufferedImage loadBackground(String path) {
        URL resource = BackgroundMenuPanel.class.getResource(path);
        if (resource == null) {
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(resource);
            if (image == null) {
                throw new IllegalStateException("Unsupported background image: " + path);
            }
            return image;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load background: " + path, exception);
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
            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            // Fill the panel while preserving the image's aspect ratio.
            double scale = Math.max(
                    (double) getWidth() / background.getWidth(),
                    (double) getHeight() / background.getHeight());
            int width = (int) Math.ceil(background.getWidth() * scale);
            int height = (int) Math.ceil(background.getHeight() * scale);
            int x = (getWidth() - width) / 2;
            int y = (getHeight() - height) / 2;
            g.drawImage(background, x, y, width, height, null);

            // Darken the image to improve the legibility of controls.
            g.setColor(new Color(0, 0, 0, 65));
            g.fillRect(0, 0, getWidth(), getHeight());
        } finally {
            g.dispose();
        }
    }
}
