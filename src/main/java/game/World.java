package game;

import file.FileManager;
import game.gameModel.Entity;
import game.gameModel.Player;
import game.gameModel.Renderable;
import game.gameModel.Scene;
import jswing.WindowManager;
import terrain.OverworldTerrain;
import terrainscene.ChunkedTerrainScene;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class World implements Serializable {
    private static final long serialVersionUID = 1L;

    private final WorldView view = new WorldView();
    private Player player;
    private Set<Entity> entities;
    List<Scene> sceneLayers;
    private String seed;
    public boolean pause = false;

    //TODO temporary
    private int tickCount = 0;

    //TODO use this world's own seed once World can provide it as a long
    private static final long TERRAIN_SEED = 42L;

    /** How many chunks of terrain are prepared beyond each edge of the screen. */
    private static final int TERRAIN_RENDER_DISTANCE = ChunkedTerrainScene.DEFAULT_RENDER_DISTANCE;

    /** Terrain tiles around the player; rebuilt from the seed, so never saved. */
    private transient ChunkedTerrainScene terrainScene;
    private transient Scene terrainLayer;

    public World(String seed) {
        this.seed = seed;
    }

    //TODO test case
    public World() {
        sample();
    }

    //TODO test case
    private void sample() {

        this.player = new Player("samplePlayer", view);
        this.sceneLayers = List.of(
                new Scene(List.of(player.stateManager.currentActionState)),
                new Scene(new HashSet<>(player.stateManager.statusStates.keySet()))
        );
        this.entities = Set.of(player);
    }

    Iterable<Renderable> render() {
        updateTerrainLayer();
        // The terrain layer goes last: the renderer's first sprites end up on top, so terrain is underneath.
        return () -> Stream.concat(StreamSupport.stream(sceneLayers.spliterator(), false), Stream.of(terrainLayer))
                .flatMap(scene -> StreamSupport.stream(
                                scene.getContent().spliterator(), false)
                        .map(renderable -> (Renderable) renderable))
                .iterator();
    }

    /** Creates the terrain layer on first use (also after loading a save) and centres it on the player. */
    private void updateTerrainLayer() {
        if (terrainScene == null) {
            terrainScene = ChunkedTerrainScene.create(
                    OverworldTerrain.fromSeed(TERRAIN_SEED),
                    WindowManager.FRAME_WIDTH, WindowManager.FRAME_HEIGHT,
                    TERRAIN_RENDER_DISTANCE);
            terrainLayer = new Scene(terrainScene);
        }
        terrainScene.update(player.coordinate[0], player.coordinate[1]);
    }

    public void takeCommand(String command) {
        switch (command) {
            case "+esc":
                this.pause = true;
                CommandProcessor.reset();
                break;
            case "+w", "-w", "+a", "-a", "+s", "-s", "+d", "-d" :
                CommandProcessor.handleDirection(command);
                break;
            default:
                CommandProcessor.handle(command);
                break;
        }
        player.handleCommand();
    }

    public void tick() {
        if (pause) return;
        CommandProcessor.synchronizeRunMode();
        player.handleCommand();
        for (Entity entity : entities) {
            entity.tick();
        }
    }

    public void loadTexture() {
        for (Entity entity : entities) {
            entity.loadTexture();
        }
    }

    public Player getPlayer() {
        return player;
    }

    public static void main(String[] args) {
        (new FileManager("sample")).saveWorld(new World());
    }


}
