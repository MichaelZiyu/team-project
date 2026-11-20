package game.interactor;

/**
 * synchronize world states with the server
 */
public interface GameSynchronizationBoundary {

    /**
     * synchronizes game state with server, including aligning entity position,
     * player registry, entity status
     */
    void synchronizeGame();
}
