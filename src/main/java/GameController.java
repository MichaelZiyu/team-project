import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


public class GameController {

    //TODO list all available commands
    enum Action {
        OPEN_BAG,
        MOVE_LEFT,
        STOP_MOVE_LEFT,
        MOVE_RIGHT,
        STOP_MOVE_RIGHT,
        JUMP,
        SLIDE,
    }

    private final Queue<InputEvent> actionBuffer = new ConcurrentLinkedQueue<>();

    //TODO commandMap to be initialized
    private final Map<Action, Command> commandMap = new HashMap<>();

    private static final long FRAME_TIME_NS = 1_000_000_000L / 60;

    public GameController() {
        initializeCommands();
    }

    private void initializeCommands() {
        commandMap.put(Action.OPEN_BAG, new OpenBagCommand());
    }


    public void onInput(InputEvent event) {
        actionBuffer.offer(event);
    }


    public void run() {
        World world = StaticView.startPanel();
        if (world == null) return;
        while (true) {

            long frameStart = System.nanoTime();

            InputEvent event;
            while ((event = actionBuffer.poll()) != null) {
                Command command = commandMap.get(toAction(event));
                if (command != null) command.execute(world);
                if (world.endgame) return;
            }
            Renderer.render(world.render()); //world.render return a renderable object or
            //data for renderer to render
            waitForNextFrame(frameStart);
        }
    }
    private Action toAction(InputEvent event) {
        if (event instanceof KeyEvent) return toAction((KeyEvent) event);
        if (event instanceof MouseEvent) return toAction((MouseEvent) event);
        return null;
    }

    private Action toAction(KeyEvent event) {

    }

    private Action toAction(MouseEvent event) {

    }


    private void waitForNextFrame(long frameStart) {
        long elapsed = System.nanoTime() - frameStart;
        long remaining = FRAME_TIME_NS - elapsed;

        if (remaining > 0) java.util.concurrent.locks.LockSupport.parkNanos(remaining);
    }


}
