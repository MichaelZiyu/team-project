package states.EntityStates.ActionStates;

import game.gameModel.Entity;
import game.gameModel.Player;

public class RunLeftState extends ActionState {

    public RunLeftState(Entity entity) {
        super("runleft", entity);
    }

    @Override
    public String handleCommand(Player.PlayerCommand command) {
        switch (command) {
            case Player.PlayerCommand.CANCEL_RUN -> {
                return "moveleft";
            }
            default -> {
                return "runleft";
            }
        }
    }
}
