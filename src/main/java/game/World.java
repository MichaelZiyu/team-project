package game;

import file.FileManager;
import game.gameModel.Entity;
import game.gameModel.Player;
import game.gameModel.Renderable;
import game.gameModel.Scene;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        return () -> StreamSupport.stream(sceneLayers.spliterator(), false)
                .flatMap(scene -> StreamSupport.stream(
                                scene.getContent().spliterator(), false)
                        .map(renderable -> (Renderable) renderable))
                .iterator();
    }

    public void takeCommand(String command) {
        switch (command) {
            case "+esc" : {
                this.pause = true;
            }
            case "+w", "-w", "+a", "-a", "+s", "-s", "+d", "-d" : {
                CommandProcessor.handleDirection(command);
            }
            default :
                CommandProcessor.handle(command);
        }
        player.handleCommand();
    }

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

    public static void main(String[] args) {
        (new FileManager("sample")).saveWorld(new World());
    }


}
