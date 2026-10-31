package game;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class Command {

    public static String translate(AWTEvent event) {
        if (event instanceof KeyEvent) {
            KeyEvent keyEvent = (KeyEvent) event;
            String key = KeyEvent.getKeyText(keyEvent.getKeyCode()).toLowerCase();

            if (keyEvent.getID() == KeyEvent.KEY_PRESSED) {
                return "+" + key;
            } else if (keyEvent.getID() == KeyEvent.KEY_RELEASED) {
                return "-" + key;
            }
        }

        else if (event instanceof MouseEvent) {
            MouseEvent mouseEvent = (MouseEvent) event;
            int button = mouseEvent.getButton();
            int id = mouseEvent.getID();

            String key;
            switch (button) {
                case MouseEvent.BUTTON1:
                    key = "left";
                    break;
                case MouseEvent.BUTTON3:
                    key = "right";
                    break;
                default:
                    key = null;
                    break;
            }

            if (key == null) return null;

            if (id == MouseEvent.MOUSE_PRESSED) {
                return "+" + key;
            } else if (id == MouseEvent.MOUSE_RELEASED) {
                return "-" + key;
            }
        }

        return null;
    }
}
