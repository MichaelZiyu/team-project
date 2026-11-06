package game.gameModel;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

/**
 * Represents a collection of Renderable objects where the order to render them does not matter.
 * Defines what it means to be a layer in 2d game.
 */
public class Scene {

    private static final Iterable<Renderable> EMPTY = Collections::emptyIterator;

    private final String name;
    private Iterable<? extends Renderable> content;
    private boolean visible = true;

    public Scene(String name, Iterable<? extends Renderable> content) {
        this.name = name;
        this.content = content;
    }

    public Scene(String name) {
        this.content = new ArrayList<>();
        this.name = name;
    }


    public Iterable<? extends Renderable> getContent() {
        if (visible) return content;
        return EMPTY;
    }

    public void setContent(Iterable<? extends Renderable> content) {
        this.content = content;
    }

    public String toString() {
        return name;
    }
}
