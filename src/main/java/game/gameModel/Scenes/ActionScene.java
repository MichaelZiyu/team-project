package game.gameModel.Scenes;

import game.gameModel.Entity;
import game.gameModel.Renderable;
import states.EntityStates.ActionStates.ActionState;
import testpackage.Test;

import java.util.Objects;
import java.util.Set;

public class ActionScene extends Scene{

    private Set<Entity> entities;

    public ActionScene(String name, Set<Entity> content) {
        super(name);
        this.entities = content;
    }

    @Override
    public Iterable<? extends Renderable> getContent() {
        if (!visible) return EMPTY;

        return () -> entities.stream()
                .map(entity -> entity.stateManager.currentActionState)
                .filter(Objects::nonNull)
                .map(state -> (Renderable) state)
                .iterator();
    }
}
