package game.controller;

import common.InputBuffer;
import game.interactor.inputBoundaries.GameCommandBoundary;
import game.interactor.inputBoundaries.GameSnapshotBoundary;
import game.interactor.inputBoundaries.GameSynchronizationBoundary;
import game.interactor.inputBoundaries.GameTickBoundary;
import game.interactor.inputData.LocalPlayerCommand;
import game.interactor.inputData.WorldState;
import game.gameOutput.GameRendererBoundary;
import game.settings.GameSettings;

import java.awt.*;
import java.awt.event.KeyEvent;
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

    private GameTickBoundary gameTick;

    private final InputBuffer localInputListener = new InputBuffer();

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
        synchronizer.synchronize(fetchWorldState());
    }

    private WorldState fetchWorldState() {
        //fetch string from server and build a WorldState object
        return null;
    }

    private void handleCommand() {
        handleLocalCommand();
        handleRemoteCommand();
    }

    private void handleLocalCommand() {
        AWTEvent event;

        while ((event = localInputListener.getInput().poll()) != null) {
            LocalPlayerCommand command = translateLocalCommand(event);
            commandBoundary.handleLocalPlayerCommand(command);
        }
    }

    private LocalPlayerCommand translateLocalCommand(AWTEvent event) {
        if (event instanceof KeyEvent) {
            KeyEvent keyEvent = (KeyEvent) event;

            String key = KeyEvent.getKeyText(keyEvent.getKeyCode())
                    .toLowerCase();

            if (keyEvent.getID() == KeyEvent.KEY_PRESSED) {
                return new LocalPlayerCommand("+" + key);
            } else if (keyEvent.getID() == KeyEvent.KEY_RELEASED) {
                return new LocalPlayerCommand("-" + key);
            }
        }

        return null;
    }

    private void handleRemoteCommand() {

    }

    private void tick() {
        gameTick.tick();
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
            tick();


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
