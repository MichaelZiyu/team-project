package game.interactor.gameModel;

import javax.swing.*;
import java.awt.image.BufferedImage;

public class Texture {

    private BufferedImage image;
    private int width, height;

    public Texture(BufferedImage image, int width, int height) {
        this.image = image;
        this.width = width;
        this.height = height;
    }
    
    public BufferedImage getImage() {
        return image;
    }

    public void tick() {

    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
}
