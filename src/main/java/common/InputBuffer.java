package common;

import java.awt.AWTEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * thread save listener of key events from jswing
 * provides getInput method to let controller consume player input
 */
public class InputBuffer implements KeyListener {
    private final Queue<AWTEvent> inputQueue =
            new ConcurrentLinkedQueue<>();

    @Override
    public void keyPressed(KeyEvent e) {
        inputQueue.offer(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        inputQueue.offer(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        inputQueue.offer(e);
    }

    public Queue<AWTEvent> getInput() {
        return inputQueue;
    }
}