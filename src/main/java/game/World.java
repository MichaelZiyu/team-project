package game;

import file.FileManager;
import game.gameModel.Player;
import game.gameModel.Renderable;
import game.gameModel.Scene;
import game.gameModel.WorldView;

import java.io.Serializable;
import java.util.List;

public class World implements Serializable {
    private static final long serialVersionUID = 1L;

    private final WorldView view = new WorldView();
    private Player player;
//    private Set<Entity> entities;
    List<Scene> sceneLayers;
    private String seed;
    public boolean pause = false;

    //TODO these two attributes are temporary
    private int tickCount = 0;
    public int time = 0;

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
    }

    List<Object> render() {
        return null;
    }

    public void takeCommand(String command) {
        switch (command) {
            case "+esc" : {
                this.pause = true;
            }
            case "+w" : {

            }
        }
    }

    public void tick() {
        if (pause) return;
        this.tickCount = (int) (tickCount + 1) % 36000;
        if ((int) (tickCount % 60) == 0) this.time += 1;
    }


}
