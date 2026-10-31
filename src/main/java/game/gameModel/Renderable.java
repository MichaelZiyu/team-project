package game.gameModel;

import file.FileManager;

import java.awt.*;
import java.io.Serializable;
import java.util.List;

/**
 * A class representing a specific map layer or background scene. It can consist of a static image, blocks,
 * or interactive components.
 */
public interface Renderable extends Serializable {


    String texturePath();

    /**
     * get texture should return what object should show right now according to its internal states and attributes
     * get texture should NOT initialize anything. initialization of objects to show should be done carefully
     * @return object that can be directly processed by the renderer chosen for the project
     */
    Object getTexture();
    int getX();
    int getY();
    int getWidth();
    int getHeight();

}
