package menu;

import entity.settings.GameSettings;
import entity.settings.GameSettings.FrameSize;
import java.util.List;
import java.util.Objects;
import menu.SettingsResult.Outcome;

/** Handles the Settings menu commands and validates a choice before applying it. */
public final class SettingsCommandHandler implements UICommandBoundary {
    private final GameSettingsAccess settingsAccess;
    private final List<FrameSize> frameSizes;

    public SettingsCommandHandler(GameSettingsAccess settingsAccess, List<FrameSize> frameSizes) {
        this.settingsAccess = Objects.requireNonNull(settingsAccess);
        this.frameSizes = List.copyOf(frameSizes);
    }

    @Override
    public void execute(UICommand command, UIListener listener) {
        Objects.requireNonNull(listener);
        Outcome outcome = switch (command) {
            case StaticUICommand staticCommand -> switch (staticCommand.action()) {
                case OPEN_SETTINGS -> Outcome.OPENED;
                case BACK -> Outcome.BACK;
            };
            case DynamicUICommand dynamicCommand -> applySettings(dynamicCommand.requestedSettings());
        };
        listener.onSettingsResult(new SettingsResult(outcome, settingsAccess.read()));
    }

    private Outcome applySettings(GameSettings requestedSettings) {
        if (!frameSizes.contains(requestedSettings.frameSize())) {
            return Outcome.UNSUPPORTED_FRAME_SIZE;
        }
        try {
            settingsAccess.apply(requestedSettings);
            return Outcome.APPLIED;
        } catch (RuntimeException failure) {
            return Outcome.APPLY_FAILED;
        }
    }
}
