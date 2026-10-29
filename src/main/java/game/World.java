package game;

import java.util.List;
import java.util.Set;

public class World {

    enum WorldView {
        TOPDOWN,
        FRONT2D
    }

    private WorldView view;

    private Player player;

    private Set<Entity> entities;

    private List<Scene> layer;


}
