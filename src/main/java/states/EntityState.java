package states;

import jswing.Texture;

import java.io.Serializable;

public class EntityState implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private int tickCount = 0;
    private transient Texture texture;
    public boolean enabled = false;

    public EntityState(String name) {
        this.name = name;
    }

    public void tick() {
        int amount = tickCount - 1;
        if (amount > 0) this.tickCount = amount;
        else {
            this.tickCount = 0;
            this.enabled = false;
        }
        texture.tick();
    }

    public void enable(int ticks) {
        this.tickCount = ticks;
        if (ticks > 0) this.enabled = true;
    }

    public void disable() {
        this.tickCount = 0;
        this.enabled = false;
    }

    public void loadTexture(String texturePath) {
        this.texture = new Texture(texturePath + "/" + name);
    }
    
    public Texture getTexture() {
        return texture;
    }
    
    public int getWidth() {
        return texture.getWidth();
    }
    
    public int getHeight() {
        return texture.getHeight();
    }
}
