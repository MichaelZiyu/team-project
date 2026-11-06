package states.EntityStates.StatusStates;

import game.gameModel.Entity;
import states.EntityStates.EntityState;

public abstract class StatusState extends EntityState {

    public StatusState(String name, Entity entity) {
        super(name, entity);
    }
}
