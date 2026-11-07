package states.EntityStates;

import game.gameModel.Entity;
import game.gameModel.Renderable;
import jswing.Texture;

import java.io.Serializable;

/**
 * Represents a certain state of an Entity, can be walking, stunning,...
 * Stores tickable sprite textures
 */
public abstract class EntityState implements Renderable, Serializable {
    private static final long serialVersionUID = 1L;

    public Entity entity;

    protected transient Texture texture;

    private int tickCount = 0;

    public EntityState(Entity entity) {
        this.entity = entity;
    }

    public void tick() {
        int amount = tickCount - 1;
        if (amount > 0) this.tickCount = amount;
        else this.tickCount = 0;

        texture.tick();
    }

    public abstract void apply();

    public void resetTickCount() {
        this.tickCount = 0;
    }

    public abstract void loadTexture(String texturePath);

    @Override
    public Texture getTexture() {
        return texture;
    }

    @Override
    public int getWidth() {
        return getTexture().getWidth();
    }

    @Override
    public int getHeight() {
        return getTexture().getHeight();
    }

    @Override
    public int getX() {
        return (int) entity.body.coordinate[0];
    }

    @Override
    public int getY() {
        return (int) entity.body.coordinate[1];
    }
}
