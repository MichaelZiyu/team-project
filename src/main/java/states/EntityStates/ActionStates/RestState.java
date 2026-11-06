package states.EntityStates.ActionStates;

import game.gameModel.Entity;
import game.gameModel.Player;
import jswing.Texture;

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
    public void apply() {
        entity.body.setVelocityX(0);
        entity.body.setVelocityY(0);
    }

    @Override
    public void loadTexture(String texturePath) {
        this.texture = new Texture(texturePath + "/" + name);
    }
}
