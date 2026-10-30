package game.gameModel;

public class PhysicsBody {

    protected double[] coordinate, velocity, acceleration;

    public PhysicsBody() {
        this.coordinate = new double[]{0.0, 0.0};
        this.velocity = new double[]{0.0, 0.0};
        this.acceleration = new double[]{0.0, 0.0};
    }
}
