package game;

import file.FileManager;
import states.PanelStateController;
import states.State;
import states.StateManager;
import system.InputBuffer;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

import jswing.Renderer;

import testpackage.Test;

public class GameController {

    private static World world;
    private static FileManager file;
    private static final long FRAME_TIME = 1_000_000_000L / 60;
    private static final JPanel mainPanel = StateManager.gameState.panel;
    private static Renderer renderer;

    public static void initializeGame(World world, FileManager file) {
        GameController.world = world;
        world.pause = false;
        GameController.file = file;
        GameController.renderer = new Renderer(mainPanel, world);
    }

    public static void run() {
        InputBuffer input = new InputBuffer();
        long lastTime = System.nanoTime();

        while (!world.pause) {

            long startTime = System.nanoTime();

            // give world legal commands observed during this frame
            for (AWTEvent event : input.getInput()) {
                String command = Command.translate(event);
                if (command != null) {
                    world.takeCommand(command);
                }
            }
            world.tick();

            long elapsed = System.nanoTime() - startTime;
            long remaining = FRAME_TIME - elapsed;
            if (remaining > 0) {
                try {
                    Thread.sleep(
                            remaining / 1_000_000,
                            (int) (remaining % 1_000_000)
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Renderer.render() is responsible for all operations that update the main panel.
            // Execute it on the EDT to ensure thread-safe Swing component updates.
            try {
                SwingUtilities.invokeAndWait(() -> {
                    renderer.render();
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (InvocationTargetException e) {
                throw new RuntimeException("rendering failed", e.getCause());
            }

        }
        PanelStateController.transition("Exit");
    }

    public static void start() {
        Thread gameThread = new Thread(GameController::run, "GameLoop");
        gameThread.start();
    }
}
