package game.gameModel.Scenes;

import game.gameModel.Entity;
import game.gameModel.Renderable;

import java.util.Set;

public class CollisionBoxScene extends Scene{

    public CollisionBoxScene(Set<Entity> entities) {
        super("CollisionBoxScene",
                () -> entities.stream()
                        .map(entity -> (Renderable) entity.body)
                        .iterator()
        );
    }

}
