package game.gameModel;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

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
