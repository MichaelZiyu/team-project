package states;

import javax.swing.*;
import java.util.Map;

/**
 * TitleState represents states that are in the title pages.
 */
public class PanelState {

    /** transition map represented by String commands and state names*/
    private Map<String, String> nameMap;

    /** state name*/
    private String name;

    public JPanel panel;


    /**
     * store its own transition function by accessing GameController, then regisiter itself to GameController.registry
     * @param name name of this state
     */
    public PanelState(String name, JPanel panel) {
        nameMap = PanelStateController.nameMap.get(name);
        this.name = name;
        this.panel = panel;
        PanelStateController.register(name, this);
    }


    /**
     * transition function of a state
     *
     * @param input transition function input
     *
     * @return the next state from transition input
     *
     */
    public PanelState transition(String input) {
        return PanelStateController.registry.get(this.nameMap.get(input));
    }

    public String toString() { return name; }
}
