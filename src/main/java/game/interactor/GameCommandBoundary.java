package game.interactor;

/**
 * handle command input boundary
 */
public interface GameCommandBoundary {

    void handleLocalPlayerCommand(LocalPlayerCommand command);

    void handleRemotePlayerCommand(RemotePlayerCommand command);
}
