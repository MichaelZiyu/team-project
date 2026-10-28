package States;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import game.Renderer;

/*
 * a state in fsm
 * every subclass of state should have class name ending with "States.State"
 * - e.g. States.StartMenuState, GamingState
 * Every state must have a final string output for the game to transit to the next state
 * Every state should store their transition maps
 */
public abstract class State {

    protected Map<String, State> transMap = new HashMap<>();
    protected Renderer renderer;

    public State(Renderer renderer) {
        this.renderer = renderer;
    }

    public State transition(String input) {return null;}

    public String run() {return null;}
}
