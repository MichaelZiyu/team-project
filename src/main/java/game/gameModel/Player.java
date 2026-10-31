package game.gameModel;

import game.WorldView;
import jswing.Texture;
import states.EntityState;

public class Player extends Entity {
    private static final long serialVersionUID = 1L;

    private static final int attack = 1;
    private static final int maxHp = 20;


    public Player(String name, WorldView view) {
        super(attack, maxHp, name, view);
    }

    @Override
    public void loadTexture() {
        for (EntityState state : stateManager.entityStates) {
            state.loadTexture(view + "/player");
        }
    }




}
