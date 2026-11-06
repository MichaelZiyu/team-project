package jswing;

import file.FileManager;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

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

    // if constructed by width and height, draw a collision box according to the inputs.
    public Texture(int width, int height) {
        JLabel label = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.RED);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };

        label.setOpaque(false);
        label.setSize(width, height);

        this.textures = new ArrayList<>(List.of(label));
        this.durations = new ArrayList<>(List.of(1));
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