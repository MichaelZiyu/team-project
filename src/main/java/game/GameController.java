package game;

import file.FileManager;
import states.*;

import jswing.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GameController {

    public static final Map<String, Map<String, String>> nameMap = FileManager.loadtransition();
    public static final Map<String, State> registry = new HashMap<>();
    public static State state = States.startMenuState;
    public static final Renderer renderer = new Renderer();


    public static void start() {
        render();
    }

    public static void transition(String input) {
        state = state.transition(input);
        if (state.panel == null) System.exit(0);
        render();
    }

    private static void render() {
        renderer.render(state);
    }
}
