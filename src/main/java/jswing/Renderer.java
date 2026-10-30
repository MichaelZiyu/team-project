package jswing;

import game.World;

import javax.swing.*;

/**
 * responsible for redrawing GameState panel according to a provided World object
 */
public class Renderer {

    private World world;
    private JPanel panel;
    JTextField text = new JTextField();


    public Renderer (JPanel panel, World world) {

        this.world = world;
        this.panel = panel;
        panel.add(text);
        text.setBounds(10,10,100,30);
    }

    public void render() {
        text.setText(world.time + "");
        panel.revalidate();
        panel.repaint();
    }

}
