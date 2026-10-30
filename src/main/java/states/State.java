package states;

import java.util.Map;

/**
 * A state in finite state machine that stores its transition function, name and corresponding panel in jswing.
 */
public abstract class State {

    /** transition map represented by String commands and state names*/
    private Map<String, String> nameMap;

    /** state name*/
    protected String name;


    /**
     * store its own transition function by accessing GameController, then regisiter itself to GameController.registry
     * @param name name of this state
     */
    public State(String name) {
        nameMap = PanelStateController.nameMap.get(name);
        this.name = name;
    }


    /**
     * transition function of a state
     *
     * @param input transition function input
     *
     * @return the next state from transition input
     *
     */
    public State transition(String input) {
        return PanelStateController.registry.get(this.nameMap.get(input));
    }

    public String toString() { return name; }
}
