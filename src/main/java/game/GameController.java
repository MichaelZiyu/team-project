package game;

import file.FileManager;
import states.*;

import jswing.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controls the whole game as a finite state machine
 */
public class GameController {

    /**
     * nameMap and registry work together to initialize all states and their transition maps
     * nameMap comes directly from transition.json, which only records the name of commands and states. Therefore
     * it requires a registration.
     * When a State object is initialized, it will store its own nameMap and register itself to registry field below.
     * So when a transition occurs, a State access its own nameMap to get the name of next state, then use registry
     * to return the next state.
     */
    public static final Map<String, Map<String, String>> nameMap = FileManager.loadtransition();
    public static final Map<String, State> registry = new HashMap<>();

    /** current state of the game*/
    public static State state = StateManager.startMenuState;

    public static final WindowManager window = new WindowManager();


    public static void start() {
        setWindow();
    }

    /**
     * transition function implementation
     * the panel of current state should be null if and only if current state is the exitState
     *
     * @param input the transition command input
     *
     */
    public static void transition(String input) {
        state = state.transition(input);
        if (state.panel == null) System.exit(0);
        setWindow();
    }

    private static void setWindow() {
        window.setWindow(state);
    }
}
