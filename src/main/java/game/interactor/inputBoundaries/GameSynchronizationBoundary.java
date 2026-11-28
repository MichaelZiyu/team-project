package game.interactor.inputBoundaries;

import game.interactor.inputData.WorldState;

/**
 * synchronize world states with the server
 */
public interface GameSynchronizationBoundary {

    /**
     * synchronizes game state with server, including aligning entity position,
     * player registry, entity status
     */
    void synchronize(WorldState state);
}
