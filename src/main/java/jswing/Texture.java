package jswing;

import file.FileManager;

import javax.swing.*;
import java.util.*;

public class Texture {

    private List<JLabel> textures;
    private List<Integer> durations;

    private int tickCount = 0;
    private int currentFrame = 0;
    private String path;

    public Texture(String texturePath) {
        Map<JLabel, Integer> loaded =
                FileManager.getListTexture(texturePath);

        this.path = texturePath;

        this.textures = new ArrayList<>(loaded.keySet());
        this.durations = new ArrayList<>(loaded.values());
    }

    public void tick() {
        tickCount++;

        if (tickCount >= durations.get(currentFrame)) {
            tickCount = 0;
            currentFrame = (currentFrame + 1) % textures.size();
        }
    }

    public JLabel getContent() {
        return textures.get(currentFrame);
    }

    public int getWidth() {
        return getContent().getIcon().getIconWidth();
    }

    public int getHeight() {
        return getContent().getIcon().getIconHeight();
    }

    public String toString() {
        return "path: " + this.path
                + "\n" + textures;
    }
}