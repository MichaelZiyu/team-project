package menu;

import entity.settings.GameSettings;
import java.util.Objects;

/** An Apply command carrying the settings selected in the menu. */
public final class DynamicUICommand extends UICommand {
    private final GameSettings requestedSettings;

    public DynamicUICommand(GameSettings requestedSettings) {
        this.requestedSettings = Objects.requireNonNull(requestedSettings);
    }

    public GameSettings requestedSettings() {
        return requestedSettings;
    }
}
