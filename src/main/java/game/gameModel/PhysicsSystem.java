package game.gameModel;

import game.GameMap;

import java.util.HashSet;
import java.util.Set;

public class PhysicsSystem {

    public static final Set<Entity> entities = new HashSet<>();
    public static final Set<PhysicsBody> entityBodies = new HashSet<>();
    public static GameMap map;

    public static void setMap(GameMap map) {
        PhysicsSystem.map = map;
    }

    public static void clear() {
        entities.clear();
        entityBodies.clear();
    }

    public static void addEntity(Entity entity) {
        entities.add(entity);
        entityBodies.add(entity.body);
    }



}
