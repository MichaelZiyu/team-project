package game.gameModel;

import java.util.Set;

public class Scene {

    private Set<Renderable> content;

    public Scene(Set<Renderable> content) {
        this.content = content;
    }

    public Set<Renderable> getContent() {
        return content;
    }
}
