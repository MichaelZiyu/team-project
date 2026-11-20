package game.interactor.gameModel.terrain;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * chuncked terrain hold a map of terrain object and coordinate to reuse created object
 * if certain terrain does not show up in the rendering distance, their corresponding coordinate
 * becomes empty
 */
public class ChunckedTerrain {

    private final Map<Terrain, Set<int[]>> chunckedTerrain = new HashMap<>();

    private TerrainGenerator generator;

    public ChunckedTerrain(long seed) {
        this.generator = new TerrainGenerator(seed);
    }
}
