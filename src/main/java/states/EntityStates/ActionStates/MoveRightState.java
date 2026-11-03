package states.EntityStates.ActionStates;

import game.gameModel.Entity;
import game.gameModel.Player;

public class MoveRightState extends ActionState{

    public MoveRightState(Entity entity) {
        super("moveright", entity);
    }

    @Override
    public String handleCommand(Player.PlayerCommand command) {
        switch (command) {
            case Player.PlayerCommand.CANCEL_AD -> {
                return "rest";
            }
            default -> {
                return "moveright";
            }
        }
    }
}
