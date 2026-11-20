package menu.gameUI;

import common.UIListener;

/**
 * called by game interactor to update the ui and enables ui telling the caller what ui
 * did through the listener
 *
 */
public interface UICommandBoundary {

    void execute(UICommand command, UIListener listener);
}
