package menu;

/** Receives the outcome of an outer-engine menu command. */
@FunctionalInterface
public interface UIListener {
    void onSettingsResult(SettingsResult result);

    default void onQuit() {
    }
}
