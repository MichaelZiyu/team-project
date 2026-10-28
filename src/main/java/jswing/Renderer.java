package jswing;

import states.State;
import states.TitleState;

import javax.swing.*;

public class Renderer {

    JFrame frame;

    public Renderer() {
        this.frame = initializeFrame();
    }

    public void render(State state) {
        if (state instanceof TitleState) {
            frame.setContentPane(((TitleState) state).panel);
            frame.revalidate();
            frame.repaint();
        }
    }

    private JFrame initializeFrame() {
        JFrame frame = new JFrame("My Game");

        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        frame.setVisible(true);

        return frame;
    }
}
