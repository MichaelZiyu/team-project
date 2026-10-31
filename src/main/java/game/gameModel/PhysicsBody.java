package game.gameModel;

import game.GameController;
import testpackage.Test;

import java.util.Arrays;

public class PhysicsBody {

    protected double[] coordinate, velocity, acceleration;

    public PhysicsBody() {
        this.coordinate = new double[]{0.0, 0.0};
        this.velocity = new double[]{0.0, 0.0};
        this.acceleration = new double[]{0.0, 0.0};
    }

    /**
     * simulate object movement for the next 16.67 ms
     */
    public void tick() {
        double time = GameController.FRAME_TIME;

        for (int i = 0; i < 2; i++) {
            coordinate[i] += velocity[i] * time
                    + 0.5 * acceleration[i] * time * time;

            velocity[i] += acceleration[i] * time;
        }
    }
}
