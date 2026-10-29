package game;

import states.State;

public abstract class Entity {
    public int hp, attack;
    public double[] coordinate, velocity;
    private State actionState, statusState;

    protected abstract void attack(Entity entity);
}
