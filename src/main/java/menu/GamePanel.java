package menu;

import game.gameOutput.GameViewBoundary.ScreenItem;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class GamePanel extends JPanel {

    // The renderer's latest frame, already placed on the screen, from the back to the front.
    private List<ScreenItem> frame = List.of();

    public void setFrame(List<ScreenItem> frame) {
        this.frame = frame;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (ScreenItem item : frame) {
            g.drawImage(
                    item.texture().getImage(),
                    item.x(),
                    item.y(),
                    item.width(),
                    item.height(),
                    null
            );
        }
    }
}
