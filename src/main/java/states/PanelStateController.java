package states;

import file.FileManager;

import jswing.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller of all PanelState, including title menus and game UIs.
 * Static class
 */
public class PanelStateController {

    /**
     * nameMap and registry work together to initialize all states and their transition maps
     * nameMap comes directly from transition.json, which only records the name of commands and states. Therefore
     * it requires a registration.
     * When a State object is initialized, it will store its own nameMap and register itself to registry field below.
     * So when a transition occurs, a State access its own nameMap to get the name of next state, then use registry
     * to return the next state.
     */
    static final Map<String, Map<String, String>> nameMap = FileManager.loadtransition();
    static final Map<String, State> registry = new HashMap<>();

    /** current state of the game*/
    private static PanelState state = StateManager.startMenuState;

    /** current JFrame window*/
    private static final WindowManager window = new WindowManager();


    public static void start() {
        setWindow();
    }

    private static void setWindow() {
        window.setWindow(state);
    }

    /**
     * transition function implementation
     * the panel of current state should be null if and only if current state is the exitState
     *
     * @param input the transition command input
     *
     */
    public static void transition(String input) {
        state = (PanelState) state.transition(input);
        if (state.panel == null) System.exit(0);
        setWindow();
    }

    public static void register(String name, State state) {
        registry.put(name, state);
    }
}
