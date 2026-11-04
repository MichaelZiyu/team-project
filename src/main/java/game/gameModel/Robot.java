package game.gameModel;

import game.WorldView;

public abstract class Robot extends Entity{


    public Robot(int attack, int maxHp, String name, WorldView view, int speed) {
        super(attack, maxHp, name, view, speed);
    }

    public abstract String getCommand();
}
