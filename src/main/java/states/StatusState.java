package states;

import game.gameModel.Entity;

public abstract class StatusState extends EntityState{

    public StatusState(String name, Entity entity) {
        super(name, entity);
    }

    public abstract void apply();
}
