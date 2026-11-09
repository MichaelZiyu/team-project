package game.gameModel;

import game.WorldView;
import game.gameModel.Physics.Collidable;
import game.gameModel.Physics.PhysicsBody;

public class Block extends Item implements Collidable {

    public Block(String name, WorldView view) {
        super(name, view);
    }

    @Override
    public PhysicsBody getBody() {
        //TODO
        return null;
    }
}
