package game.gameModel;

import game.World;
import jswing.WindowManager;
import terrain.OverworldTerrain;
import terrain.Trees;
import terrainscene.ChunkedTerrainScene;
import terrainscene.TreePainter;
import terrainscene.TreeScene;

import java.util.Iterator;

public class GameMap implements Iterable<Renderable>{

    private long seed;
    private Player player;
    private ChunkedTerrainScene terrain;
    private TreeScene trees;
    private long lastTick = System.nanoTime();

    public GameMap(long seed, Player player) {
        this.seed = seed;
        this.player = player;
        // One terrain shared by the ground and the trees growing on it.
        OverworldTerrain ground = OverworldTerrain.fromSeed(seed);
        Trees growing = new Trees(ground, seed);
        // The trees' shadows are painted into the terrain.
        this.terrain = ChunkedTerrainScene.create(ground, growing, WindowManager.FRAME_WIDTH,
                WindowManager.FRAME_HEIGHT, World.TERRAIN_RENDER_DISTANCE);
        this.trees = new TreeScene(growing, new TreePainter(),
                WindowManager.FRAME_WIDTH, WindowManager.FRAME_HEIGHT);
    }

    public void tick() {
        double x = player.body.coordinate[0];
        double y = player.body.coordinate[1];
        terrain.update(x, y);
        long now = System.nanoTime();
        double seconds = Math.min(0.1, (now - lastTick) / 1e9);
        lastTick = now;
        int[] size = playerSize();
        trees.update(x, y, size[0], size[1], seconds);
    }

    /** The trees behind the player, drawn under the characters. */
    public Iterable<Renderable> treesBehind() {
        return trees.behind();
    }

    /** The trees in front of the player, drawn over the characters. */
    public Iterable<Renderable> treesInFront() {
        return trees.inFront();
    }

    /** The player's sprite's size; its image can be missing for a moment while it changes. */
    private int[] playerSize() {
        try {
            Renderable sprite = player.stateManager.currentActionState;
            if (sprite != null && sprite.getTexture() != null) {
                return new int[] {sprite.getWidth(), sprite.getHeight()};
            }
        } catch (RuntimeException e) {
            // Mid-change on another thread: use the usual size this tick.
        }
        return new int[] {64, 64};
    }

    @Override
    public Iterator<Renderable> iterator() {
        return terrain.iterator();
    }
}
