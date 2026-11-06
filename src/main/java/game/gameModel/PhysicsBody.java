package game.gameModel;

import game.GameController;
import jswing.Texture;
import testpackage.Test;

import java.io.Serializable;

public class PhysicsBody implements Serializable, Renderable {
    private static final long serialVersionUID = 1L;

    public double[] coordinate, velocity, acceleration;

    private int width, height;

    private transient Texture texture;

    private double runCoefficient = 1.0;

    public PhysicsBody(int width, int height) {
        this.width = width;
        this.height = height;
        loadTexture();
        this.coordinate = new double[]{0.0, 0.0};
        this.velocity = new double[]{0.0, 0.0};
        this.acceleration = new double[]{0.0, 0.0};
    }

    public void setVelocityX(double speed) {
        velocity[0] = speed;
    }

    public void setVelocityY(double speed) {
        velocity[1] = speed;
    }

    public void setRunCoefficient(double multiplier) {
        runCoefficient = multiplier;
    }

    public void resetRunCoefficient() {
        runCoefficient = 1;
    }

    /**
     * simulate object movement for the next 16.67 ms
     */
    public void tick() {
        double time = GameController.FRAME_TIME;
        double p, v, a;
        for (int i = 0; i < 2; i++) {
            p = coordinate[i];
            v = velocity[i];
            a = acceleration[i];
            p += (v * runCoefficient) * time
                    + 0.5 * a * time * time;

            v += a * time;
            coordinate[i] = p;
            velocity[i] = v;
        }
    }

    public void loadTexture() {
        this.texture = new Texture(width, height);
    }

    @Override
    public int getX() {
        return (int) coordinate[0];
    }

    @Override
    public int getY() {
        return (int) coordinate[1];
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public Texture getTexture() {
        return texture;
    }

}
