package game.gameModel.Physics;

import game.World;
import game.gameModel.Block;
import game.gameModel.GameMap;
import game.gameModel.Entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PhysicsSystem implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final double ENTITY_COLLISION_MAX_SPEED = 80.0;
    private static final double ENTITY_COLLISION_AREA_SCALE = 400.0;

    private final List<Collidable> collidingPairs = new ArrayList<>();
    private final List<Double> collisionAreas = new ArrayList<>();

    private World world;

    public PhysicsSystem(World world) {
        this.world = world;
    }

    /**
     * Handle collisions from entities to entities or from entities to blocks by setting entities positions
     * and/or velocities.
     */
    public void handleCollision() {
        detectCollision();
        applyEffect();
    }

    /**
     * Detects collisions between Collidables in the system.
     *
     * The returned list is flattened: every two consecutive elements
     * represent one colliding pair.
     * If it is a block-entity collision, block is always a first.
     * There is no block-block collision in this game.
     *
     * @return a flat list of colliding Collidables, with each pair appearing
     *         exactly once.
     */
    private void detectCollision() {
        List<Entity> entityList = new ArrayList<>(world.entities);
        collidingPairs.clear();
        collisionAreas.clear();
        double collisionArea;

        for (int i = 0; i < entityList.size(); i++) {
            entityList.get(i).body.resetCollisionVelocity();
            for (int j = i + 1; j < entityList.size(); j++) {
                collisionArea = getIntersectArea(entityList.get(i).body, entityList.get(j).body);
                if (collisionArea > 0) {
                    collidingPairs.add(entityList.get(i));
                    collidingPairs.add(entityList.get(j));
                    collisionAreas.add(collisionArea);
                }
            }
        }
    }

    private void applyEffect() {
        Collidable x,y;
        int collisionAreaIndex = 0;
        for (int i = 0; i < collidingPairs.size(); i += 2) {
            x = collidingPairs.get(i);
            y = collidingPairs.get(i+1);
            if (x instanceof Entity) {
                applyEntityCollision((Entity) x, (Entity) y, collisionAreas.get(collisionAreaIndex));
            } else if (x instanceof Block) {
                // TODO entity-block collision
            }
            collisionAreaIndex += 1;
        }
    }

    /**
     * ASSUMPTION: PhysicsBody.coordinate represent buttom-centered position.
     *
     * calculate and apply the effect of this entity collision
     *
     * @param x entity x
     * @param y entity y
     */
    private void applyEntityCollision(Entity x, Entity y, double area) {

        double speed = ENTITY_COLLISION_MAX_SPEED
                * (1 - Math.exp(-area / ENTITY_COLLISION_AREA_SCALE));

        double dx = x.body.coordinate[0] - y.body.coordinate[0];

        double dy = (x.body.coordinate[1] + x.body.getHeight() / 2.0)
                - (y.body.coordinate[1] + y.body.getHeight() / 2.0);

        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance == 0) {
            return;
        }

        double nx = dx / distance;
        double ny = dy / distance;

        x.body.setCollisionVelocity(nx * speed, ny * speed);
        y.body.setCollisionVelocity(-nx * speed, -ny * speed);
    }

    private double getIntersectArea(PhysicsBody x, PhysicsBody y) {

        double leftX = Math.max(
                x.coordinate[0] - x.getWidth() / 2.0,
                y.coordinate[0] - y.getWidth() / 2.0
        );

        double rightX = Math.min(
                x.coordinate[0] + x.getWidth() / 2.0,
                y.coordinate[0] + y.getWidth() / 2.0
        );

        double bottomY = Math.max(x.coordinate[1], y.coordinate[1]);

        double topY = Math.min(
                x.coordinate[1] + x.getHeight(),
                y.coordinate[1] + y.getHeight()
        );

        double width = Math.max(0, rightX - leftX);
        double height = Math.max(0, topY - bottomY);

        return width * height;
    }
}
