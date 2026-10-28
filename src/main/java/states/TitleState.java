package states;

import game.GameController;

import javax.swing.*;

public class TitleState extends State{

    public TitleState(String name, JPanel panel){
        super(name, panel);
        this.name = name;
    }

    @Override
    public State transition(String input) {
        return GameController.registry.get(this.transMap.get(input));
    }

    public String toString() {
        return name;
    }
}
