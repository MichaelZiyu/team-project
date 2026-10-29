package states;

import jswing.PanelManager;


/**
 * Static class responsible for initializing, storing, and managing all game states.
 * Provides public access to predefined states.
 */
public class StateManager {

    public static final TitleState startMenuState = new TitleState("startMenuState", PanelManager.startMenuPanel);
    public static final TitleState settingMenuState = new TitleState("settingMenuState",
            PanelManager.settingMenuPanel);
    public static final TitleState exitState = new TitleState("exitState", null);


}
