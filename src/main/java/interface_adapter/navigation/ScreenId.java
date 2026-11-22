package interface_adapter.navigation;

/**
 * Presentation-layer identifier for an application screen.
 * The navigation view model publishes these values, and the Swing navigation
 * host uses them to find the corresponding panel.
 */

public enum ScreenId {
    TITLE,
    SAVES,
    NEW_WORLD,
    SETTINGS,
    GAME
}
