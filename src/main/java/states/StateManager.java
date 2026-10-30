package states;

import jswing.PanelManager;


/**
 * Static class responsible for initializing, storing, and managing all game states.
 * Provides public access to predefined states.
 */
public class StateManager {

    public static final PanelState startMenuState = new PanelState("startMenuState", PanelManager.startMenuPanel);
    public static final PanelState settingMenuState = new PanelState("settingMenuState",
            PanelManager.settingMenuPanel);
    public static final PanelState exitState = new PanelState("exitState", null);
    public static final PanelState loadState = new PanelState("loadMenuState", PanelManager.loadMenuPanel);
    public static final PanelState gameState = new PanelState("gameState", PanelManager.gamePanel);


}
