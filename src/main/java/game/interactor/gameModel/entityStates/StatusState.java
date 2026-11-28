package game.interactor.gameModel.entityStates;

import game.interactor.gameModel.Texture;

public abstract class StatusState {

    Texture texture;

    public void tick() {
        texture.tick();
    }
}
