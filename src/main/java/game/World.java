package game;

import file.FileManager;
import game.gameModel.*;
import game.gameModel.Physics.PhysicsSystem;
import game.gameModel.Scenes.*;
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
    // TODO: this is a configurable constant
    public static int TERRAIN_RENDER_DISTANCE = 1;

    //TEMP: test case
    private static final long TERRAIN_SEED = 42L;
    private long seed;

    public Player player;
    public Set<Entity> entities = new HashSet<>();

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

    private PhysicsSystem physicsSystem;

    private transient BackGround background;

    public World(long seed) {

        //TODO: test case
        seed = TERRAIN_SEED;

        this.seed = seed;
        this.view = new WorldView();
        this.player = new Player("player1", view);
        this.map = new GameMap(seed, player);
        this.entities.add(player);

        //TEMP:test case
        entities.add(new Player("player2", view));

        initialize();
    }

    /**
     * initialize not serializable attributes
     */
    public void initialize() {
        loadTexture();
        this.map = new GameMap(seed, player);
        this.background = new BackGround(player, view, WindowManager.FRAME_WIDTH, WindowManager.FRAME_HEIGHT);
        initializeSceneLayers();
        this.physicsSystem = new PhysicsSystem(this);
    }

    /**
     * initialize scene layers
     * scenes come earlier in the list are scenes on the top of the view.
     */
    private void initializeSceneLayers() {
        this.sceneLayers = List.of(
                new Scene("gameUI"), // pause menu

                new CollisionBoxScene(entities), // collision box

                new Scene("filters"), // such as weather, sunlight

                new Scene("entityStats"), // hp

                new StatusScene(entities), // character status textures
                new ActionScene(entities), // character movement textures

                new Scene("interactable"), // cave
                new Scene("terrain", map), // terrain map

                new Scene("Background", List.of(background)) // background picture
        );
        // hide the background as it is only used in front 2d.
        sceneLayers.get(8).setVisible(false);
        // TODO only delete this after status state finished
        sceneLayers.get(3).setVisible(false);

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
        player.handleCommand();
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
        physicsSystem.handleCollision();
        Test.print(this);
    }

    public void loadTexture() {
        for (Entity entity : entities) {
            entity.loadTexture();
        }
    }

    public Player getPlayer() {
        return player;
    }

    public void switchView() {
        view.switchView();
    }

    public String toString() {
        String result = "";
        for (Entity entity : entities) {
            result += entity;
        }
        return result;
    }


}
