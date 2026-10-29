package states;

import game.World;

import javax.swing.*;

public class GameState extends State{

    public World world;

    public GameState(String name, JPanel panel, World world) {
        super(name, panel);
        this.world = world;
    }
}
