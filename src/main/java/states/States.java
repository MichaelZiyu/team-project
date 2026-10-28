package states;

import jswing.Panels;

import java.util.List;


/**
 * Static class responsible for initializing, storing, and managing all game states.
 * Provides public access to predefined states and methods for organizing and retrieving
 * collections of states.
 */

public class States {

    public static final TitleState startMenuState = new TitleState("startMenuState", Panels.startMenuPanel);
    public static final TitleState settingMenuState = new TitleState("settingMenuState", Panels.settingMenuPanel);
    public static final TitleState exitState = new TitleState("exitState", null);

}
