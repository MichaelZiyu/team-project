package system;

import java.awt.*;
import java.util.*;
import java.util.List;

//TODO this class is temporarily useless
public class InputBuffer {
    private final List<AWTEvent> input = new ArrayList<>();

    public InputBuffer() {
        // Listen to keyboard events throughout the application
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(event -> {
                    input.add(event);
                    return false; // Do not consume the event
                });

        // Listen to mouse events throughout the application
        Toolkit.getDefaultToolkit().addAWTEventListener(
                event -> input.add(event),
                AWTEvent.MOUSE_EVENT_MASK
                        | AWTEvent.MOUSE_MOTION_EVENT_MASK
                        | AWTEvent.MOUSE_WHEEL_EVENT_MASK
        );
    }

    public List<AWTEvent> getInput() {
        List<AWTEvent> result = new ArrayList<>(input);
        input.clear();
        return result;
    }
}