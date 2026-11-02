import states.PanelStateController;
import javax.swing.SwingUtilities;
import testpackage.Test;

public class Main extends Test { //testpackage.Test is just a toolkit

    public static void main(String[] args){
        SwingUtilities.invokeLater(PanelStateController::start);
    }

}

/**
 * a list of configurable constants(updated 11/1):
 * WindowManager.FRAME_WIDTH
 * WindowManager.FRAME_HEIGHT
 * GameController.FRAME_TIME
 */

