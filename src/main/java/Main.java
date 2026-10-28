import States.*;
import game.Panels;

import javax.swing.*;

public class Main {

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Game");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800,600);
            frame.setLocationRelativeTo(null);

            frame.setContentPane(Panels.startMenuPanel);
            frame.setVisible(true);
        });
        State currentState = new StartMenuState();
//        while (currentState != null) {
//            currentState = currentState.transition(currentState.run());
//        }
    }
}
