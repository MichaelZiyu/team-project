package states;

import game.GameController;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/*
 * a state in fsm
 * every subclass of state should have class name ending with "States.State"
 * - e.g. States.StartMenuState, GamingState
 * Every state must have a final string output for the game to transit to the next state
 * Every state should store their transition maps
 */
public abstract class State {

    protected Map<String, String> transMap;
    public JPanel panel;
    protected String name;


    public State(String key, JPanel panel) {
        transMap = GameController.nameMap.get(key);
        register(key);
        this.panel = panel;
        this.name = key;
    }

    private void register(String key) {
        GameController.registry.put(key, this);
    }

    public abstract State transition(String input);

    public String toString() {
        return name;
    }
}
