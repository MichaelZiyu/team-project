package game.interactor.gameModel.entityStates;

import game.interactor.gameModel.Texture;

public abstract class MovementState {

    Texture texture;

    public void tick() {
        texture.tick();
    }
}
