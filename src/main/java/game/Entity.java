package game;

import States.State;

public abstract class Entity {
    int hp, attack;
    double x, y, z;
    State actionState, statusState;
}
