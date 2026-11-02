package states.EntityStates.ActionStates;

import game.gameModel.Entity;
import game.gameModel.Player;
import states.EntityStates.EntityState;

public abstract class ActionState extends EntityState {

    public ActionState(String name, Entity entity) {
        super(name, entity);
    }

    public abstract String handleCommand(Player.PlayerCommand command);
}
