package game.interactor.gameModel.scene;

import game.interactor.World;
import game.interactor.gameModel.entities.Entity;
import game.interactor.inputData.WorldState;
import game.serverAccess.NetworkSerializable;
import game.serverAccess.ServerSenderData;

import java.util.HashSet;
import java.util.Set;

public class Entities extends Scene implements NetworkSerializable {

    private Set<Entity> entities = new HashSet<>();

    @Override
    public void tick(World world) {
        for (Entity entity : entities) {
            entity.tick();
        }
    }

    @Override
    public ServerSenderData networkSerialize() {
        WorldState state = new WorldState(entities);
        return new ServerSenderData(state);
    }

    public void addEntity(Entity entity)
    {
        entities.add(entity);
    }
}
