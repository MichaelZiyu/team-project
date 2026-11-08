package game.gameModel.Scenes;

import game.WorldView;
import game.gameModel.Player;
import game.gameModel.Renderable;
import jswing.Texture;

public class BackGround implements Renderable {

    private WorldView view;
    private int windowWidth, windowHeight;
    private Player player;

    private Texture texture;

    public BackGround(Player player, WorldView view, int windowWidth, int windowHeight) {
        this.view = view;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.player = player;

        this.texture = new Texture(view + "/background");
    }

    @Override
    public Texture getTexture() {
        return texture;
    }

    @Override
    public int getX() {
        return (int) player.body.coordinate[0];
    }

    @Override
    public int getY() {
        return (int) player.body.coordinate[1] - (windowHeight / 2);
    }

    @Override
    public int getWidth() {
        return windowWidth;
    }

    @Override
    public int getHeight() {
        return windowHeight;
    }

}
