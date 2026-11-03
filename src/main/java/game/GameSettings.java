package game;

/** Application settings shared by the UI and game thread for the current session. */
public final class GameSettings {
    public enum RunMode { HOLD, TOGGLE }
    private static volatile RunMode runMode = RunMode.HOLD;

    private GameSettings() { }

    public static RunMode getRunMode() { return runMode; }

    public static void setRunMode(RunMode mode) {
        if (mode == null) { throw new IllegalArgumentException("Run mode is required."); }
        runMode = mode;
    }
}
