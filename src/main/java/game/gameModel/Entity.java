package game.gameModel;

import states.EntityState;

import java.io.Serializable;

public abstract class Entity extends PhysicsBody implements Renderable, Serializable {
    private static final long serialVersionUID = 1L;

    protected int hp, attack, maxHp;

    private EntityState state;
    private WorldView view;

    protected boolean dead = false;
    protected Inventory inventory;
    protected int tick = 0;

    protected Object texture;

    public Entity(int attack, int maxHp, String name, WorldView view) {
        super();
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.attack = attack;
        this.inventory = new Inventory();
        this.view = view;
        loadTexture();
    }

    @Override
    public String texturePath() {
        return view + "/" + state;
    }

    private void loadTexture() {

    }

    /**
     * get texture that the entity should show right now, depending on current state and tick
     * @return
     */
    @Override
    public Object getTexture() {
        return null;
    }


}
