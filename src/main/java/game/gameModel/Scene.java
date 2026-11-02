package game.gameModel;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

public class Scene implements Serializable {
    private static final long serialVersionUID = 1L;

    private Iterable<? extends Renderable> content;

    public Scene(Iterable<? extends Renderable> content) {
        this.content = content;
    }

    public Iterable<? extends Renderable> getContent() {
        return content;
    }
}
