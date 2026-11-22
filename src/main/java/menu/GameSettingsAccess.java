package menu;

import entity.settings.GameSettings;

/** Connects the settings use case to the application that owns these settings. */
public interface GameSettingsAccess {
    GameSettings read();

    /** Applies the supplied settings or throws if applying them fails. */
    void apply(GameSettings requestedSettings);
}
