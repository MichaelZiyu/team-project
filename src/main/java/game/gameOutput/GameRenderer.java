package game.gameOutput;

import game.interactor.outputData.RenderableData;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Renders the game by rendering a snapshot of the game on a separate thread and using
 * AtomicReference to update the snapshot
 */
public class GameRenderer implements GameRendererBoundary {

    private final AtomicReference<RenderableData> snapshot =
            new AtomicReference<>();

    private GameViewBoundary gameViewBoundary;

    private volatile boolean running = true;

    @Override
    public void submitSnapshot(RenderableData data) {
        snapshot.set(data);
    }

    /**
     * called by renderer thread created by main thread, try to render the last submitted snapshot
     * as soon as possible
     */
    public void run() {
        while (running) {
            RenderableData data = snapshot.get();

            if (data != null) {
                render(data);
            }
        }
    }

    /**
     * stops rendering
     *
     * usually stops when game ui is opened or when user exit the game
     */
    public void stop() {
        running = false;
    }

    /**
     * render provided renderable data and update GameViewModel
     * @param data
     */
    private void render(RenderableData data) {
            // Rendering implementation
            // swingutilities invoke later, call a boundary from menu/command to let jswing update the panel
            // provides renderable item to the boundary
            SwingUtilities.invokeLater(() -> {
                gameViewBoundary.updateScreen(data);
            });
        }
}