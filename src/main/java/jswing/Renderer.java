package jswing;

import game.World;

/**
 * responsible for redrawing GameState panel according to a provided World object
 */
public class Renderer {

    World world;

    public Renderer (World world) {
        this.world = world;
    }
}
