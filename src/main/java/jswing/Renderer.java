package jswing;

import game.gameModel.Renderable;

import javax.swing.*;
import java.awt.*;

/**
 * responsible for redrawing GameState panel according to a provided World object
 */
public class Renderer {

    private JPanel panel;
    private Camera camera = new Camera();

    public Renderer (JPanel panel) {
        this.panel = panel;
    }

    public void render(Iterable<? extends Renderable> renderables, int playerX, int playerY) {
        panel.removeAll();

        for (Renderable renderable : renderables) {
            Component component = renderable.getTexture().getContent();

            int width = renderable.getWidth();
            int height = renderable.getHeight();

            // Apply transformation to coordinates through camera
            camera.transform(playerX, playerY, renderable.getX(), renderable.getY(),
                    renderable.getWidth(), renderable.getHeight());
            component.setBounds(
                    camera.x,
                    camera.y,
                    width,
                    height
            );

            panel.add(component);
        }

        // Debug: display player position
        camera.transform(playerX, playerY, playerX, playerY, 5, 5);

        JPanel debugPoint = new JPanel();
        debugPoint.setBackground(Color.BLACK);
        debugPoint.setBounds(camera.x, camera.y, 5, 5);
        panel.add(debugPoint);

        panel.revalidate();
        panel.repaint();
    }

}
