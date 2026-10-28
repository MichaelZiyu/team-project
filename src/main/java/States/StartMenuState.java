package States;


import game.Panels;
import game.Renderer;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class StartMenuState extends State {

    Map<String, State> transMap = new HashMap<>();

    public StartMenuState(Renderer renderer) {
        super(renderer);
    }

    @Override
    public String run() {
        // output(render), do anything you need, and return a String key for the next state
        renderer.render(Panels.startMenuPanel);
        return null;
    }

    @Override
    public State transition(String input) {
        return null;
    }
}
