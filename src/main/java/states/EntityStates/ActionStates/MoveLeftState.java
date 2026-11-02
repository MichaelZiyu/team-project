package states.EntityStates.ActionStates;

import game.gameModel.Entity;
import game.gameModel.Player;

public class MoveLeftState extends ActionState {

    public MoveLeftState(Entity entity) {
        super("moveleft", entity);
    }

    public String handleCommand(Player.PlayerCommand command) {
        switch (command) {
            case Player.PlayerCommand.CANCEL_AD -> {
                return "rest";
            }
            default -> {
                return "moveleft";
            }
        }
    }
}
