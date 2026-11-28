package game.data;

import game.interactor.gameModel.Texture;

public interface LoadTextureBoundary {

    Texture loadTexture(String texturePath, int width, int height);
}
