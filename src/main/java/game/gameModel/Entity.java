package game.gameModel;

import game.WorldView;
import jswing.Texture;
import states.EntityStates.EntityStateManager;
import testpackage.Test;

import java.io.Serializable;

/**
 * Anything
 */
public abstract class Entity implements Serializable, Renderable{
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
        registerPhysicsSystem();
    }

    public void tick() {
        body.tick();
        stateManager.tick();
    }

    public void registerPhysicsSystem() {
        PhysicsSystem.addBody(body);
    }

    @Override
    public int getX() {
        return body.getX();
    }

    @Override
    public int getY() {
        return body.getY();
    }

    @Override
    public int getWidth() {
        return body.getWidth();
    }

    @Override
    public int getHeight() {
        return body.getHeight();
    }

    @Override
    public Texture getTexture() {
        return stateManager.getTexture();
    }

    public abstract void loadTexture();

    public abstract void handleCommand();

}
