package menu;

import game.gameOutput.GameViewBoundary;
import game.interactor.outputData.RenderableData;

public class GameView implements GameViewBoundary {

    private final GamePanel gamePanel;

    public GameView(GamePanel panel) {
        this.gamePanel = panel;
    }

    @Override
    public void updateSnapshot(RenderableData data) {
        gamePanel.setSnapshot(data);
        gamePanel.repaint();
    }
}
