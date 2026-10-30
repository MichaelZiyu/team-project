package game.gameModel;

import java.awt.*;
import java.io.Serializable;

/**
 * A class representing a specific map layer or background scene. It can consist of a static image, blocks,
 * or interactive components.
 */
public abstract class Scene implements Serializable {
    private static final long serialVersionUID = 1L;

    public WorldView view;

    public Scene(WorldView view) {
        this.view = view;
    }

    public abstract Component render();
}
