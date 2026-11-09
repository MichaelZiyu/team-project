import file.FileManager;
import game.World;
import states.PanelStateController;
import testpackage.Test;

public class Main {

    public static void main(String[] args){

        // TEMP: test case
        // save a sample world
        World world = new World(42L);
        new FileManager("sample").saveWorld(world);
        PanelStateController.start();
    }

}

/**
 * a list of configurable constants(updated 11/1):
 * WindowManager.FRAME_WIDTH
 * WindowManager.FRAME_HEIGHT
 * GameController.FRAME_TIME
 */

//TODO issue: when player1 is behind player2 it is still rendered as above player2


