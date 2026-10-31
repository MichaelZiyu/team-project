package game.gameModel;

import game.WorldView;
import jswing.Texture;
import states.EntityState;
import states.EntityStateManager;

import java.io.Serializable;
import java.util.Set;

public abstract class Entity extends PhysicsBody implements Renderable, Serializable {
    private static final long serialVersionUID = 1L;

    int hp, attack, maxHp;
    String name;

    EntityStateManager stateManager = new EntityStateManager();
    EntityState currentState = stateManager.rest;
    WorldView view;

    Inventory inventory;

    public Entity(int attack, int maxHp, String name, WorldView view) {
        super();
        this.name = name;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.attack = attack;
        this.inventory = new Inventory();
        this.view = view;
        loadTexture();
    }

    public abstract void loadTexture();

    @Override
    public int getX() {
        return (int) this.coordinate[0];
    }

    @Override
    public int getY() {
        return (int) this.coordinate[1];
    }

    @Override
    public int getWidth() {
        return currentState.getWidth();
    }

    @Override
    public int getHeight() {
        return currentState.getHeight();
    }

    @Override
    public Texture getTexture() {
        return currentState.getTexture();
    }


}
