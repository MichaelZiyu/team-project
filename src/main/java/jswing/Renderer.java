package jswing;

import game.gameModel.Renderable;

import javax.swing.*;

/**
 * responsible for redrawing GameState panel according to a provided World object
 */
public class Renderer {

    private JPanel panel;

    public Renderer (JPanel panel) {
        this.panel = panel;
    }

    public void render(Iterable<Renderable> renderables) {
        panel.revalidate();
        panel.repaint();
    }

}
