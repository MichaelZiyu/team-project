package game.gameModel;

import java.io.Serializable;

public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    protected int hp, attack, maxHp;
    protected double[] coordinate = {0.0, 0.0, 0.0};
    protected double[] velocity = {0.0, 0.0, 0.0};
//    private ActionState actionState;
//    private StatusState statusState;
    protected boolean dead = false;

    protected Inventory inventory;

    protected WorldView view;

    public Entity(int attack, int maxHp, WorldView view) {
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.attack = attack;
        this.inventory = new Inventory();
        this.view = view;
    }


    void attack(Entity entity) {
        entity.injure(attack, this);
    }

    void injure(int damage, Entity source) {

    }

//    void setStatusState(StatusState)

    public boolean isDead() {
        return dead;
    }

    public void tp(double[] coordinate) {
        this.coordinate = coordinate;
    }
}
