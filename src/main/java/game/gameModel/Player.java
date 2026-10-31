package game.gameModel;

import game.CommandProcessor;
import game.WorldView;
import states.EntityState;
import testpackage.Test;

import java.util.Arrays;

public class Player extends Entity {
    private static final long serialVersionUID = 1L;

    private static final int ATTACK = 1;
    private static final int MAX_HP = 20;
    private static final int SPEED = 1;


    public Player(String name, WorldView view) {
        super(ATTACK, MAX_HP, name, view, SPEED);
    }

    @Override
    public void loadTexture() {
        for (EntityState state : stateManager.entityStates) {
            state.loadTexture(view + "/player");
        }
    }

    public void handleCommand() {
        char ws = CommandProcessor.currentInput[0];
        char ad = CommandProcessor.currentInput[1];

        // Here w is in negative direction of y because in jswing the y-axis is in opposite direction
        if (ws == 'w') velocity[1] -= SPEED;
        else if (ws == 's') velocity[1] += SPEED;
        else velocity[1] = 0;

        if (ad == 'a') velocity[0] -= SPEED;
        else if (ad == 'd') velocity[0] += SPEED;
        else velocity[0] = 0;
    }

    public void tick() {
        super.tick();
        Test.print(Arrays.toString(coordinate));
    }




}
