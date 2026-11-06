package game;

import file.FileManager;
import game.gameModel.*;
import jswing.WindowManager;
import testpackage.Test;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * Represents the internal states of the game
 */
public class World implements Serializable {
    private static final long serialVersionUID = 1L;


    // view saves the type of view the game is currently in
    private WorldView view;

    // rendering distance for terrain chunks.
    // TODO: this is a configurable constant and should show up in setting menu
    public static int TERRAIN_RENDER_DISTANCE = 1;

    //TEMP: test case
    private static final long TERRAIN_SEED = 42L;
    private long seed;

    private Player player;
    private Set<Entity> entities = new HashSet<>();

    /**
     * sceneLayers is a list of ordered Scenes for the game to render. Each Scene object stores
     * an Iterable of Renderables.
     *
     * sceneLayers consists of several fixed layers:
     * - Background
     * - ground, including blocks, interactable blocks(e.g. cave)
     * - interactable items
     * - physicsbody of all entities
     * - action state texture of all entities(e.g. walking, attacking)
     * - status state texture of all entities(e.g. stunning, freeze)
     * - entity stats(e.g. player stats, mob hp)
     * - weather effects and sunlight
     * - game ui panels
     *
     * The scenes rendered earlier will be behind the scenes rendered after.
     */
    public transient List<Scene> sceneLayers;

    // GameController will check if this is true in each loop, and stop the loop if end = true.
    public transient boolean end = false;

    // Terrain object generates the blocks on the ground through a String seed
    // also includes interactable blocks like caves
    // GameMap is an Iterable<Renderable>
    private transient GameMap map;

    private transient BackGround background;

    public World(long seed) {

        //TODO: test case
        seed = TERRAIN_SEED;

        this.seed = seed;
        this.view = new WorldView();
        this.player = new Player("playername", view);
        this.map = new GameMap(seed, player);
        this.entities.add(player);

        initialize();
    }

    public static void main(String[] args) {
        World world = new World(1);
        new FileManager("sample").saveWorld(world);
    }

    /**
     * initialize not serializable attributes
     */
    public void initialize() {
        loadTexture();
        this.map = new GameMap(seed, player);
        this.background = new BackGround(player, view, WindowManager.FRAME_WIDTH, WindowManager.FRAME_HEIGHT);
        initializeSceneLayers();
    }

    /**
     * initialize scene layers
     * Prerequisite:
     * - background, map are correctly initialized
     */
    private void initializeSceneLayers() {
        this.sceneLayers = List.of(
                new Scene("Background", List.of(background)), // background picture
                new Scene("terrain", map), // terrain map
                new Scene("interactable"), // cave
                new Scene("physicsbody"), // collision box
                new Scene("entities"), // characters, status effect
                new Scene("entityStats"), // hp
                new Scene("filters"), // such as weather, sunlight
                new Scene("gameUI") // pause menu
        );
        
        //TEMP:
        sceneLayers.get(0).setVisible(false);
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
                this.end = true;
            }
            case "+e" -> {

            }
            case "right" -> {
                //
            }
            case "+f" -> {
                // interact
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
        if (end) return;
        map.tick();
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


}
