package states.EntityStates.ActionStates;

import game.gameModel.Entity;
import game.gameModel.Player;

public class MoveBackwardState extends ActionState{

    public MoveBackwardState(Entity entity) {
        super("movebackward", entity);
    }

    @Override
    public String handleCommand(Player.PlayerCommand command) {
        switch (command) {
            case Player.PlayerCommand.CANCEL_WS -> {
                return "rest";
            }
            default -> {
                return "movebackward";
            }
        }
    }
}
