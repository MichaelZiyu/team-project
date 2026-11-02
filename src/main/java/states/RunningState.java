package states;

import game.gameModel.Entity;
import game.gameModel.Player;

public class RunningState extends ActionState{

    public RunningState(Entity entity) {
        super("running", entity);
    }

    @Override
    public ActionState handleCommand(Player.PlayerCommand command) {
        return null;

    }
}
