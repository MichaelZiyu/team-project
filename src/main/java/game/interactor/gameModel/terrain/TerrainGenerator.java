package game.interactor.gameModel.terrain;

/**
 * provide world a function that maps an int[] coordinate to a terrain object
 */
public class TerrainGenerator {

    private long seed;

    public TerrainGenerator(long seed) {
        this.seed = seed;
    }

    public Terrain getTerrain(int[] coordinate) {
        return null;
    }
}
