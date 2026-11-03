package states.EntityStates.ActionStates;

import game.gameModel.Entity;
import game.gameModel.Player;

public class MoveForwardState extends ActionState{

    public MoveForwardState(Entity entity) {
        super("moveforward", entity);
    }

    @Override
    public String handleCommand(Player.PlayerCommand command) {
        switch (command) {
            case Player.PlayerCommand.CANCEL_WS -> {
                return "rest";
            }
            default -> {
                return "moveforward";
            }
        }
    }
}
