import states.PanelStateController;
import javax.swing.SwingUtilities;
import testpackage.Test;

public class Main extends Test { //testpackage.Test is just a toolkit

    public static void main(String[] args){
        SwingUtilities.invokeLater(PanelStateController::start);
    }

}
