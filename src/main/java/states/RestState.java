package states;

import game.gameModel.Entity;
import game.gameModel.Player;

public class RestState extends ActionState{

    public RestState(Entity entity) {
        super("rest", entity);
    }

    @Override
    public ActionState handleCommand(Player.PlayerCommand command) {
        return this;
    }
}
