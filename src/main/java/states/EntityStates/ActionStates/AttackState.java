package states.EntityStates.ActionStates;

import game.gameModel.Entity;
import game.gameModel.Player;

//TODO implement attack state
public class AttackState extends ActionState{

    public static final String name = "attack";

    public AttackState(Entity entity) {
        super(entity);
    }

    @Override
    public String handleCommand(Player.PlayerCommand command) {
        switch (command) {
        }
        //TEMP:
        return RestState.name;
    }

    //TODO
    @Override
    public void apply() {}
    //TODO
    @Override
    public void loadTexture(String texturePath) {

    }
}
