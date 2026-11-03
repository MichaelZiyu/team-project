package game;

/** Shift edge detection; independent of Swing, player textures and frame rate. */
final class RunInput {
    private GameSettings.RunMode mode = GameSettings.RunMode.HOLD;
    private boolean shiftDown;
    private boolean running;

    boolean setMode(GameSettings.RunMode newMode) {
        if (mode == newMode) { return false; }
        mode = newMode;
        reset();
        return true;
    }

    void press() {
        if (shiftDown) { return; } // Ignore OS key-repeat.
        shiftDown = true;
        running = mode == GameSettings.RunMode.HOLD || !running;
    }

    void release() {
        shiftDown = false;
        if (mode == GameSettings.RunMode.HOLD) { running = false; }
    }

    void reset() {
        shiftDown = false;
        running = false;
    }

    boolean isRunning() { return running; }
}
