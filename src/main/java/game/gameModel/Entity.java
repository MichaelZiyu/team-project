package game.gameModel;

import game.WorldView;
import game.gameModel.Physics.PhysicsBody;
import states.EntityStates.EntityStateManager;

import java.io.Serializable;

/**
 * Anything
 */
public abstract class Entity implements Serializable{
    private static final long serialVersionUID = 1L;

    public int hp, attack, maxHp, speed;
    String name;
    public static final double RUN_COEFFICIENT = 3;

    public PhysicsBody body;

    public EntityStateManager stateManager = new EntityStateManager(this);
    WorldView view;

    Inventory inventory;

    public Entity(int attack, int maxHp, String name, WorldView view, int speed, int width, int height) {
        this.name = name;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.attack = attack;
        this.inventory = new Inventory();
        this.view = view;
        this.speed = speed;
        this.body = new PhysicsBody(width, height);
        loadTexture();
    }

    public void tick() {
        body.tick();
        stateManager.tick();
    }

    public abstract void loadTexture();

    public abstract void handleCommand();

}
