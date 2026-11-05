package game;

import game.gameModel.Player;
import game.gameModel.Renderable;
import terrainscene.ChunkedTerrainScene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class GameMap implements Iterable<Renderable>{

    private long seed;
    private Player player;
    private ChunkedTerrainScene terrain;

    public GameMap(long seed, Player player) {
        this.seed = seed;
        this.player = player;
        this.terrain = new ChunkedTerrainScene(seed);
    }

    public void tick() {
        terrain.update(player.coordinate[0], player.coordinate[1]);
    }

    @Override
    public Iterator<Renderable> iterator() {
        return Collections.emptyIterator();
    }
}
