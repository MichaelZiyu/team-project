package menu;

import entity.settings.GameSettings;

/** The outcome and applied settings returned to the menu. */
public record SettingsResult(Outcome outcome, GameSettings appliedSettings) {
    public enum Outcome {
        OPENED,
        APPLIED,
        BACK,
        UNSUPPORTED_FRAME_SIZE,
        APPLY_FAILED
    }
}
