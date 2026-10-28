package game;

import javax.swing.*;
import java.util.List;

public class Renderer {

    private final JPanel container;

    public Renderer(JPanel container) {
        this.container = container;
    }

    public void render(List<JPanel> panels) {
        container.removeAll();

        for (JPanel panel : panels) {
            container.add(panel);
        }

        container.revalidate();
        container.repaint();
    }

    public void render(JPanel panel) {
        render(List.of(panel));
    }
}
