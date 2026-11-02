package game.gameModel;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

public class Scene implements Serializable {
    private static final long serialVersionUID = 1L;

    private Set<Renderable> content;

    public Scene(Set<Renderable> content) {
        this.content = content;
    }

    public Set<Renderable> getContent() {
        return content;
    }
}
