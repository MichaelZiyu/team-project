package states;

import game.gameModel.Entity;
import game.gameModel.Player;

public abstract class ActionState extends EntityState{

    public ActionState(String name, Entity entity) {
        super(name, entity);
    }

    public abstract ActionState handleCommand(Player.PlayerCommand command);
}
