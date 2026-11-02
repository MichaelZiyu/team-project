package game.gameModel;

import game.GameController;
import testpackage.Test;

import java.io.Serializable;

public class PhysicsBody implements Serializable {
    private static final long serialVersionUID = 1L;

    public double[] coordinate, velocity, acceleration;

    private double runCoefficient = 1.0;

    public PhysicsBody() {
        this.coordinate = new double[]{0.0, 0.0};
        this.velocity = new double[]{0.0, 0.0};
        this.acceleration = new double[]{0.0, 0.0};
    }

    void setVelocityX(double speed) {
        velocity[0] = speed;
    }

    void setVelocityY(double speed) {
        velocity[1] = speed;
    }

    void setRunCoefficient(double multiplier) {
        runCoefficient = multiplier;
    }

    void resetRunCoefficient() {
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
}
