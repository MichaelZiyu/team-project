package game.gameModel;

import game.CommandProcessor;
import game.WorldView;
import testpackage.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Player extends Entity {
    private static final long serialVersionUID = 1L;

    private static final int ATTACK = 1;
    private static final int MAX_HP = 20;
    private static final int SPEED = 120;
    private static final int PLAYER_BODY_WIDTH = 40;
    private static final int PLAYER_BODY_HEIGHT = 64;

    public enum PlayerCommand {
        // Movements
        MOVE_FORWARD,
        MOVE_BACKWARD,
        MOVE_LEFT,
        MOVE_RIGHT,
        CANCEL_FORWARD,
        CANCEL_BACKWARD,
        CANCEL_LEFT,
        CANCEL_RIGHT,

        RUN,
        CANCEL_RUN,

        // Menus
        OPEN_INVENTORY,
        PAUSE,

        // Actions
        ATTACK,

        // Other
        INTERACT
    }

    public Player(String name, WorldView view) {
        super(ATTACK, MAX_HP, name, view, SPEED, PLAYER_BODY_WIDTH, PLAYER_BODY_HEIGHT);
    }

    @Override
    public void loadTexture() {
        body.loadTexture();
        stateManager.loadTexture(view + "/player");
    }

    @Override
    public void handleCommand() {
        PlayerCommand command;

        while ((command = CommandProcessor.commandQueue.poll()) != null) {
            stateManager.handleCommand(command);
        }
        stateManager.apply();
    }


}
