package game.controller;

import menu.inputData.InitializeGameData;

/**
 * provides a run method for EDT thread to start the game
 */
public interface GameControllerBoundary {

    /**
     * run the game
     */
    void run();

    /**
     * initialize the game with InitializeGameData
     */
    void initializeGame (InitializeGameData data);
}
