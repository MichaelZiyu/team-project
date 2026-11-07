package game.gameModel;

import game.GameMap;
import terrainscene.ChunkedTerrainScene;

import java.util.HashSet;
import java.util.Set;

public class PhysicsSystem {

    public static final Set<PhysicsBody> bodies = new HashSet<>();
    public static GameMap map;

    public static void setMap(GameMap map) {
        PhysicsSystem.map = map;
    }



}
