package states.EntityStates.ActionStates;

import game.gameModel.Entity;
import game.gameModel.Player;
import jswing.Texture;

public class RestState extends ActionState {

    public static final String name = "rest";

    public RestState(Entity entity) {
        super(entity);
    }

    @Override
    public String handleCommand(Player.PlayerCommand command) {
        switch (command) {
            case Player.PlayerCommand.MOVE_FORWARD,
                 Player.PlayerCommand.MOVE_BACKWARD,
                 Player.PlayerCommand.MOVE_LEFT,
                 Player.PlayerCommand.MOVE_RIGHT -> {return MovingState.name + "-" + command.name();}
            default -> {
                return name;
            }
        }
    }

    @Override
    public void apply() {
        entity.body.setVelocityX(0);
        entity.body.setVelocityY(0);
    }

    @Override
    public void loadTexture(String texturePath) {
        this.texture = new Texture(texturePath + "/" + name);
    }

    public String toString() {
        return name;
    }
}
