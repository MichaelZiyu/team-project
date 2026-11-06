import file.FileManager;
import game.World;
import states.PanelStateController;
import testpackage.Test;

public class Main extends Test { //testpackage.Test is just a toolkit

    public static void main(String[] args){
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

//TODO: make PhysicsBody, Entity and StateManager renderable.

