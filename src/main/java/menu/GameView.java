package menu;

import game.gameOutput.GameViewBoundary;

import java.util.List;

public class GameView implements GameViewBoundary {

    private final GamePanel gamePanel;

    public GameView(GamePanel panel) {
        this.gamePanel = panel;
    }

    @Override
    public void show(List<ScreenItem> frame) {
        gamePanel.setFrame(frame);
        gamePanel.repaint();
    }
}
