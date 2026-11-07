package game.gameModel.Scenes;

import game.gameModel.Entity;
import game.gameModel.Renderable;
import states.EntityStates.StatusStates.StatusState;

import java.util.Set;

public class StatusScene extends Scene{

    private Set<Entity> entities;

    public StatusScene(String name, Set<Entity> content) {
        super(name);
        this.entities = content;
    }

    @Override
    public Iterable<? extends Renderable> getContent() {
        if (!visible) return EMPTY;

        return () -> entities.stream()
                .flatMap(entity -> entity.stateManager.currentStatusStates.stream())
                .map(state -> (Renderable) state)
                .iterator();
    }
}
