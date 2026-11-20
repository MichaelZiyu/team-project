package menu.gameUI;

import common.UIListener;

// called by game thread to update the ui and enables ui tell the caller what ui did through listener
public interface UICommandBoundary {

    void execute(UICommand command, UIListener listener);
}
