package states;

import game.GameController;

import javax.swing.*;

/**
 * TitleState represents states that are in the title menus, which is different from GameState
 */
public class TitleState extends State{

    public TitleState(String name, JPanel panel){
        super(name, panel);
    }
}
