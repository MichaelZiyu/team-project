package states.EntityStates.ActionStates;

import game.gameModel.Entity;
import game.gameModel.Player;

public class RestState extends ActionState {

    public RestState(Entity entity) {
        super("rest", entity);
    }

    @Override
    public String handleCommand(Player.PlayerCommand command) {
        switch (command) {
            case Player.PlayerCommand.MOVE_LEFT -> {
                return "moveleft";
            }
            default -> {
                return "rest";
            }
        }
    }
}
