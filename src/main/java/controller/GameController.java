package controller;

import interactor.GameCommandBoundary;
import interactor.GameSnapshotBoundary;
import interactor.GameSynchronizationBoundary;
import presenter.GameRendererBoundary;
import settings.GameSettings;

import java.util.concurrent.locks.LockSupport;

public class GameController implements GameControllerBoundary{

    private GameCommandBoundary commandBoundary;
    private GameSynchronizationBoundary synchronizer;
    private GameRendererBoundary gameRenderer;
    private GameSnapshotBoundary gameSnapshot;
    // game snapshot knows an input data structure that game renderer relies on

    private void render() {
        // renderer maintains an atomic reference inside class.
        // .submitSnapshot method submit the newest snapshot to the renderer
        gameRenderer.submitSnapshot(gameSnapshot.getSnapshot());
    }

    private void synchronize() {
        synchronizer.synchronizeGame();
    }

    private void handleCommand() {

        commandBoundary.handleLocalPlayerCommand();

        commandBoundary.handleServerPlayerCommand();

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
