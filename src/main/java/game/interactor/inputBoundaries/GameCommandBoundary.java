package game.interactor.inputBoundaries;

import game.interactor.inputData.LocalPlayerCommand;
import game.interactor.inputData.RemotePlayerCommand;

/**
 * handle command input boundary
 */
public interface GameCommandBoundary {

    void handleLocalPlayerCommand(LocalPlayerCommand command);

    void handleRemotePlayerCommand(RemotePlayerCommand command);
}
