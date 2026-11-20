package game.controller;

import game.interactor.GameCommandBoundary;
import game.interactor.GameSnapshotBoundary;
import game.interactor.GameSynchronizationBoundary;
import game.presenter.GameRendererBoundary;
import game.settings.GameSettings;

import java.util.concurrent.locks.LockSupport;

/**
 * Controller of the game
 * Responsible for controlling frame rate, calling renderer, synchronize local and remote game data,
 * and let the game handle command from players
 */
public class GameController implements GameControllerBoundary{

    private GameCommandBoundary commandBoundary;
    private GameSynchronizationBoundary synchronizer;

    private GameRendererBoundary gameRenderer;
    private GameSnapshotBoundary gameSnapshot;
    // game snapshot knows an input data structure that game renderer relies on

    /**
     * submit a game snapshot to renderer
     * for more details about renderer, see GameRendererBoundary
     */
    private void render() {
        // renderer maintains an atomic reference inside class.
        // .submitSnapshot method submit the newest snapshot to the renderer
        gameRenderer.submitSnapshot(gameSnapshot.getSnapshot());
    }

    private void synchronize() {
        synchronizer.synchronizeGame();
    }

    private void handleCommand() {
        commandBoundary.handleCommand();
    }

    /**
     * run the game by having a loop that updates the game 60 fps
     */
    @Override
    public void run() {
        long nextFrame = System.nanoTime();
        long remainder = 0;

        final long frameTime = 1_000_000_000L / GameSettings.FPS;
        final long frameRemainder = 1_000_000_000L % GameSettings.FPS;

        while (true) {

            // the only execution part
            render();
            synchronize();
            handleCommand();


            nextFrame += frameTime;
            remainder += frameRemainder;

            if (remainder >= GameSettings.FPS) {
                nextFrame++;
                remainder -= GameSettings.FPS;
            }

            long remaining;

            while ((remaining = nextFrame - System.nanoTime()) > 0) {
                LockSupport.parkNanos(remaining);

                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
            }
        }
    }
}
