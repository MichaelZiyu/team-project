package game.gameModel;

import game.WorldView;
import jswing.Texture;
import states.EntityState;
import states.EntityStateManager;

import java.io.Serializable;
import java.util.Set;

public abstract class Entity extends PhysicsBody {
    private static final long serialVersionUID = 1L;

    int hp, attack, maxHp, speed;
    String name;

    public EntityStateManager stateManager = new EntityStateManager(this);
    WorldView view;

    Inventory inventory;

    public Entity(int attack, int maxHp, String name, WorldView view, int speed) {
        super();
        this.name = name;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.attack = attack;
        this.inventory = new Inventory();
        this.view = view;
        this.speed = speed;
        loadTexture();
    }

    public abstract void loadTexture();

}
