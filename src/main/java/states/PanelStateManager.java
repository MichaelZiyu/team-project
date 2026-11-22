package states;

import jswing.PanelManager;


/** Registers the panel instances assembled at startup. */
public class PanelStateManager {

    final PanelState startMenuState;
    final PanelState loadMenuState;
    final PanelState newWorldState;
    final PanelState gameState;

    PanelStateManager(PanelManager panels) {
        startMenuState = new PanelState("startMenuState", panels.startMenuPanel);
        loadMenuState = new PanelState("loadMenuState", panels.loadMenuPanel);
        newWorldState = new PanelState("generateMenuState", panels.newWorldPanel);
        gameState = new PanelState("gameState", panels.gamePanel);
        new PanelState("settingMenuState", panels.settingMenuPanel);
        new PanelState("exitState", null);
    }

}
