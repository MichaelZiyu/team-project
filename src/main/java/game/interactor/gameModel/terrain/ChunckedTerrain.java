package game.interactor.gameModel.terrain;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * chuncked terrain hold a map of terrain object and coordinate to reuse created object
 * if certain terrain does not show up in the rendering distance, their corresponding coordinate
 * becomes empty.
 *
 * Therefore, all type of terrains are loaded at once and the only differences as the game continues
 * are their coordinates.
 *
 * ChunckedTerrain also holds a generator that whenever it updates according to player position,
 * it asks the generator where to put which type of terrain.
 */
public class ChunckedTerrain {

    private final Map<Terrain, Set<int[]>> chunckedTerrain = new HashMap<>();

    private TerrainGenerator generator;

    public ChunckedTerrain(long seed) {
        this.generator = new TerrainGenerator(seed);
    }
}
