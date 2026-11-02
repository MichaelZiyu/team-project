package game.gameModel;

import game.CommandProcessor;
import game.WorldView;
import testpackage.Test;

import java.util.Arrays;

public class Player extends Entity {
    private static final long serialVersionUID = 1L;

    private static final int ATTACK = 1;
    private static final int MAX_HP = 20;
    private static final int SPEED = 120;
    private static final double RUN_COEFFICIENT = 3;

    public enum PlayerCommand {
        // Movements
        MOVE_FORWARD,
        MOVE_BACKWARD,
        MOVE_LEFT,
        MOVE_RIGHT,
        CANCEL_WS,
        CANCEL_AD,

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
        super(ATTACK, MAX_HP, name, view, SPEED);
    }

    @Override
    public void loadTexture() {
        stateManager.loadTexture(view + "/player");
    }

    public void handleCommand() {
        PlayerCommand command;

        while ((command = CommandProcessor.commandQueue.poll()) != null) {
            switch (command) {
                case MOVE_FORWARD -> setVelocityY(SPEED);
                case MOVE_BACKWARD -> setVelocityY(-SPEED);
                case MOVE_LEFT -> setVelocityX(-SPEED);
                case MOVE_RIGHT -> setVelocityX(SPEED);
                case CANCEL_WS -> setVelocityY(0);
                case CANCEL_AD -> setVelocityX(0);
                case RUN -> setRunCoefficient(RUN_COEFFICIENT);
                case CANCEL_RUN -> resetRunCoefficient();
            }

            stateManager.handleCommand(command);        }
    }

    public void tick() {
        super.tick();
        stateManager.tick();
    }




}
