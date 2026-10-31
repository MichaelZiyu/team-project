package jswing;

import file.FileManager;

import javax.swing.*;
import java.util.List;

public class Texture {

    private List<JLabel> textures;
    private int tickCount = 0;
    private int maxTick;

    /**
     * initialize textures according to the texture path.
     * @param texturePath a path relative to resources/textures that specifies where a list of textures are stored.
     */
    public Texture(String texturePath) {
        this.textures = FileManager.getListTexture(texturePath);
        this.maxTick = textures.size() - 1;
    }

    public void tick() {
        this.tickCount = (tickCount + 1) % maxTick;
    }

    public JLabel getContent() {
        return textures.get(tickCount);
    }

    public int getWidth() {
        return getContent().getIcon().getIconWidth();
    }

    public int getHeight() {
        return getContent().getIcon().getIconHeight();
    }
}
