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
            case Player.PlayerCommand.MOVE_FORWARD,
                 Player.PlayerCommand.MOVE_BACKWARD,
                 Player.PlayerCommand.MOVE_LEFT,
                 Player.PlayerCommand.MOVE_RIGHT -> {return "movement-" + command.name();}
            default -> {
                return "rest";
            }
        }
    }

    @Override
    public void apply() {}
}
