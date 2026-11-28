package game.interactor.gameModel.entities;

import game.interactor.gameModel.Inventory;
import game.interactor.gameModel.entityStates.EntityStateMachine;
import game.serverAccess.NetworkSerializable;
import game.serverAccess.ServerSenderData;
import game.settings.GameSettings;

import java.io.Serializable;

public abstract class Entity extends PhysicsBody implements Serializable, EntityObject{
    private static final long serialVersionUID = 1L;

    EntityStats stats;

    Inventory inventory;

    double runCoefficient = GameSettings.ENTITY_RUN_COEFFICIENT;

    EntityStateMachine stateManager = new EntityStateMachine();

    @Override
    public void tick() {
        super.tick();
        stateManager.tick();
    }

    public abstract void loadTexture();

    @Override
    public float[] getLocation() {
        return stats.getLocation();
    }

    @Override
    public String getID() {
        return stats.getId();
    }
}
