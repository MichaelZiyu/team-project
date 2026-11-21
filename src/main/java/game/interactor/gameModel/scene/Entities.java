package game.interactor.gameModel.scene;

import game.interactor.World;
import game.interactor.gameModel.entities.Entity;

import java.util.HashSet;
import java.util.Set;

public class Entities extends Scene {

    private Set<Entity> entities = new HashSet<>();

    @Override
    public void tick(World world) {
        for (Entity entity : entities) {
            entity.tick();
        }
    }
}
