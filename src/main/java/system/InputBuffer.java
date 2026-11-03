package system;

import java.awt.*;
import java.awt.event.AWTEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Thread-safe EDT-to-game-thread event queue with explicit listener cleanup. */
public class InputBuffer implements AutoCloseable {
    private final Queue<AWTEvent> input = new ConcurrentLinkedQueue<>();
    private final KeyEventDispatcher keys = event -> {
        input.offer(event);
        return false;
    };
    private final AWTEventListener events = input::offer;

    public InputBuffer() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keys);
        Toolkit.getDefaultToolkit().addAWTEventListener(events,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
                        | AWTEvent.MOUSE_WHEEL_EVENT_MASK | AWTEvent.WINDOW_FOCUS_EVENT_MASK);
    }

    public List<AWTEvent> getInput() {
        List<AWTEvent> result = new ArrayList<>();
        AWTEvent event;
        while ((event = input.poll()) != null) { result.add(event); }
        return result;
    }

    @Override public void close() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keys);
        Toolkit.getDefaultToolkit().removeAWTEventListener(events);
        input.clear();
    }
}
