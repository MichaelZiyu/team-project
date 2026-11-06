package game.gameModel;

import game.WorldView;
import jswing.Texture;

public class BackGround implements Renderable{

    private WorldView view;
    private int windowWidth, windowHeight;

    private Texture texture;

    public BackGround(WorldView view, int windowWidth, int windowHeight) {
        this.view = view;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;

        this.texture = new Texture(view + "/background");
    }

    @Override
    public Texture getTexture() {
        return texture;
    }

    @Override
    public int getX() {
        return windowWidth / 2;
    }

    @Override
    public int getY() {
        return - (windowHeight / 2);
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
