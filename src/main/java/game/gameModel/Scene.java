package game.gameModel;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * Represents a collection of Renderable objects where the order to render them does not matter.
 * Defines what it means to be a layer in 2d game.
 */
public class Scene implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final Iterable<? extends Renderable> content;

    public Scene(String name, Iterable<? extends Renderable> content) {
        this.name = name;
        this.content = content;
    }

    public Iterable<? extends Renderable> getContent() {
        return content;
    }

    public String toString() {
        return name;
    }
}
