package menu;

import game.interactor.outputData.RenderableData;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {

    private RenderableData snapshot;

    public void setSnapshot(RenderableData snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (snapshot == null) {
            return;
        }

        for (RenderableData.RenderableItem item : snapshot) {
            g.drawImage(
                    item.texture().getImage(),
                    item.x(),
                    item.y(),
                    item.texture().getWidth(),
                    item.texture().getHeight(),
                    null
            );
        }
    }
}