package game;

import game.gameModel.Player;
import game.gameModel.Scene;
import game.gameModel.WorldView;

import java.io.Serializable;
import java.util.List;

public class World implements Serializable {
    private static final long serialVersionUID = 1L;

    private WorldView view;

    private Player player;
//
//    private Set<Entity> entities;

    private List<Scene> sceneLayers;
    private String seed;

    public World(String seed) {
        this.seed = seed;
    }

    public World() {
        sample();
    }

    private void sample() {
        this.view = new WorldView();
        this.player = new Player("samplePlayer", view);
    }


}
