package game.gameModel;

import java.util.HashSet;
import java.util.Set;

public class PhysicsSystem {

    public static final Set<PhysicsBody> bodies = new HashSet<>();


    public static void addBody(PhysicsBody body) {
        bodies.add(body);
    }

    public static void clear() {
        bodies.clear();
    }

}
