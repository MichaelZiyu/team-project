package game;

import file.FileManager;
import game.gameModel.Entity;
import game.gameModel.Player;
import game.gameModel.Renderable;
import game.gameModel.Scene;
import terrainscene.ChunkedTerrainScene;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * Represents the internal states of the game
 */
public class World implements Serializable {
    private static final long serialVersionUID = 1L;


    // view saves the type of view the game is currently in
    private final WorldView view = new WorldView();

    // rendering distance for terrain chunks.
    // TODO: this is a configurable constant and should show up in setting menu
    private static int TERRAIN_RENDER_DISTANCE = ChunkedTerrainScene.DEFAULT_RENDER_DISTANCE;



    private Player player;
    private Set<Entity> entities;

    /**
     * sceneLayers is a list of ordered Scenes for the game to render. Each Scene object stores
     * an Iterable of Renderables.
     *
     * sceneLayers consists of several fixed layers:
     * - 1. Background
     * - 2. ground, including blocks, interactable blocks(e.g. cave)
     * - 3. physicsbody of all entities
     * - 4. action state texture of all entities(e.g. walking, attacking)
     * - 5. status state texture of all entities(e.g. stunning, freeze)
     * - 6. entity stats(e.g. player stats, mob hp)
     * - 7. weather effects and sunlight
     *
     * The scenes rendered earlier will be behind the scenes rendered after.
     */
    public List<Scene> sceneLayers;

    // GameController will check if this is true in each loop, and stop the loop if paused.
    public boolean pause = false;

    // Terrain object generates the blocks on the ground through a String seed
    private Terrain terrain;

    // render distance controls how many chuncks the game render dynamically
    public static final double RENDER_DISTANCE = 16;

    public World(String seed) {
        this.terrain = new Terrain(seed);
    }

    // TEMP: test sample
    // save a sample world file
    private static void sample() {

        World w = new World("seed");
        Player player = new Player("sample", new WorldView());
        w.sceneLayers = List.of(
                new Scene("playerActionLayer", player.stateManager.currentActionState),
                new Scene("playerStatusLayer", player.stateManager.currentStatusStates)
        );
        w.player = player;
        w.entities = Set.of(player);
        new FileManager("sample").saveWorld(w);
    }

    /**
     * Provide Iterable<Renderable> generated through scene.getContent for Renderer to render in O(1)
     *
     * @return Iterable<Renderable> contents in scenes stored in sceneLayers
     *
     */
    Iterable<Renderable> render() {
        return () -> StreamSupport.stream(sceneLayers.spliterator(), false)
                .flatMap(scene -> StreamSupport.stream(
                                scene.getContent().spliterator(), false)
                        .map(renderable -> (Renderable) renderable))
                .iterator();
    }

    /**
     * Handle all the command and determine where to put the command
     *
     * @param command a String command filtered by CommandProcessor
     */
    public void handleCommand(String command) {
        switch (command) {
            case "+esc" -> {
                this.pause = true;
            }
            case "+w", "-w", "+a", "-a", "+s", "-s", "+d", "-d" -> {
                CommandProcessor.handleDirection(command);
            }
            default ->
                CommandProcessor.handle(command);
        }
        for (Entity entity : entities) {
            entity.handleCommand();
        }
    }

    /**
     * Make the game move to the next moment
     */
    public void tick() {
        if (pause) return;
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

    // TEMP: test
    public static void main(String[] args) {
        sample();
    }


}
