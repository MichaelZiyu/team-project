package states;

import game.GameController;

import javax.swing.*;
import java.util.Map;

/**
 * A state in finite state machine that stores its transition function, name and corresponding panel in jswing.
 */
public abstract class State {

    /** transition map represented by String commands and state names*/
    protected Map<String, String> nameMap;

    /** the panel for current state. It is null if and only if this state is the exitState*/
    public JPanel panel;

    /** state name*/
    protected String name;


    /**
     * store its own transition function by accessing GameController, then regisiter itself to GameController.registry
     * @param name name of this state
     * @param panel panel of this state
     */
    public State(String name, JPanel panel) {
        nameMap = GameController.nameMap.get(name);
        this.name = name;
        register();
        this.panel = panel;
    }

    /**
     * register this to GameController.registry
     */
    private void register() {
        GameController.registry.put(name, this);
    }

    /**
     * transition function of a state
     * @param input transition function input
     * @return the next state from transition input
     */
    public State transition(String input) {
        return GameController.registry.get(this.nameMap.get(input));
    }

    public String toString() { return name; }
}
