package game.gameModel;

public class Player extends Entity {
    private static final long serialVersionUID = 1L;

    private static final int maxHp = 20;
    private static int attack = 1;
    private String name;


    public Player(String name, WorldView view) { // should also accept a texture but we'll do it later
        super(attack, maxHp, view);
        this.name = name;
    }


}
