package menu;

import game.gameOutput.GameViewBoundary;

import java.util.List;

public class GameView implements GameViewBoundary {

    private final GamePanel gamePanel;
    private final GameAudio audio;

    public GameView(GamePanel panel) {
        this(panel, new GameAudio());
    }

    public GameView(GamePanel panel, GameAudio audio) {
        this.gamePanel = panel;
        this.audio = audio;
    }

    @Override
    public void show(List<ScreenItem> frame) {
        gamePanel.setFrame(frame);
        gamePanel.repaint();
    }

    @Override
    public void playSound(List<Loop> loops, List<OneShot> now) {
        audio.update(loops, now);
    }
}
