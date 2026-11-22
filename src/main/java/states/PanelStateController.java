package states;

import file.FileManager;

import game.GameController;
import interface_adapter.navigation.NavigationViewModel;
import interface_adapter.navigation.ScreenId;
import jswing.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller of all PanelState, including title menus and game UIs. Controls the entire game as a finite state machine.
 * Controls all page transitions by providing static and public attributes and methods
 */
public class PanelStateController {

    /**
     * nameMap and registry work together to initialize all states and their transition maps
     * nameMap comes directly from transition.json, which only records the name of commands and states. Therefore
     * it requires a registration.
     * When a PanelState object is initialized, it will store its own nameMap and register itself to registry field below.
     * So when a transition occurs, a State access its own nameMap to get the name of next state, then use registry
     * to return the next state as a PanelState object.
     */
    static final Map<String, Map<String, String>> nameMap = FileManager.loadtransition();
    static final Map<String, PanelState> registry = new HashMap<>();

    /** current state of the entire game*/
    private static PanelState state;
    private static PanelStateManager states;

    /** current JFrame window*/
    private static WindowManager window;


    /** Registers the supplied panels and opens the current menu screen. */
    public static void start(PanelManager panels, NavigationViewModel navigation) {
        states = new PanelStateManager(panels);
        window = new WindowManager();
        navigation.addScreenListener(PanelStateController::showMenu);
    }

    private static void showMenu(ScreenId screen) {
        state = switch (screen) {
            case TITLE -> states.startMenuState;
            case SAVES -> states.loadMenuState;
            case NEW_WORLD -> states.newWorldState;
        };
        setWindow();
    }

    private static void setWindow() {
        window.setWindow(state);
    }

    public static void setWindowSize(int width, int height) {
        window.setWindowSize(width, height);
    }

    /**
     * transition function implementation
     * the panel of current state should be null if and only if current state is the exitState
     *
     * @param input the transition command input
     *
     */
    public static void transition(String input) {
        PanelStateController.state = state.transition(input);
        if (state.panel == null) System.exit(0);
        setWindow();
        if (state == states.gameState) {
            GameController.start();
        }
    }

    /**register provided name and panel states to registry*/
    public static void register(String name, PanelState state) {
        registry.put(name, state);
    }
}
