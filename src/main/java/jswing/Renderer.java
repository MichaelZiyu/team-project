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

        panel.revalidate();
        panel.repaint();
    }

}
