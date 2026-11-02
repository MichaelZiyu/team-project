package states;

import game.gameModel.Entity;
import game.gameModel.Renderable;
import jswing.Texture;
import testpackage.Test;

import java.io.Serializable;

/**
 * Represents a certain state of an Entity, can be walking, stunning,...
 * Stores tickable sprite textures
 */
public class EntityState implements Renderable, Serializable {
    private static final long serialVersionUID = 1L;

    String name;
    Entity entity;

    transient Texture texture;

    private int tickCount = 0;

    public EntityState(String name, Entity entity) {
        this.name = name;
        this.entity = entity;
    }

    public void tick() {
        int amount = tickCount - 1;
        if (amount > 0) this.tickCount = amount;
        else {
            this.tickCount = 0;
        }
        texture.tick();
    }

    public void resetTickCount() {
        this.tickCount = 0;
    }

    public void loadTexture(String texturePath) {
        this.texture = new Texture(texturePath + "/" + name);
    }

    @Override
    public Texture getTexture() {
        return texture;
    }

    @Override
    public int getWidth() {
        return texture.getWidth();
    }

    @Override
    public int getHeight() {
        return texture.getHeight();
    }

    @Override
    public int getX() {
        return (int) entity.coordinate[0];
    }

    @Override
    public int getY() {
        return (int) entity.coordinate[1];
    }
}
