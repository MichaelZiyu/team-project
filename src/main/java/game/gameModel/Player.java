package game.gameModel;

import game.WorldView;

public class Player extends Entity {
    private static final long serialVersionUID = 1L;

    private static int attack = 1;
    private static final int maxHp = 20;


    public Player(String name, WorldView view) {
        super(attack, maxHp, name, view);
    }


}
