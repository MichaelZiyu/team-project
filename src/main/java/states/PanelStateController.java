package states;

import java.util.Objects;

/** Forwards the existing gameplay exit callback to the outer engine. */
public final class PanelStateController {
    private static Runnable onGameExit;

    private PanelStateController() {
    }

    /** Connects the callback during application startup on the Swing event thread. */
    public static void setGameExitHandler(Runnable handler) {
        onGameExit = Objects.requireNonNull(handler);
    }

    /** Entry point used by the game loop when it returns to the menu. */
    public static void transition(String command) {
        if (!"Exit".equals(command)) {
            throw new IllegalArgumentException("Unsupported gameplay command: " + command);
        }
        if (onGameExit == null) {
            throw new IllegalStateException("Gameplay exit handler has not been set");
        }
        onGameExit.run();
    }
}
