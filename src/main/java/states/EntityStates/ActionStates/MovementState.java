package states.EntityStates.ActionStates;

import game.gameModel.Entity;
import game.gameModel.Player;
import jswing.Texture;
import testpackage.Test;

import java.util.HashMap;
import java.util.Map;

public class MovementState extends ActionState{

    private transient Map<String, Texture> textureMap;

    private String currentws, previousws, currentad, previousad, newest;

    public MovementState(Entity entity) {
        super("movement", entity);
    }

    @Override
    public String handleCommand(Player.PlayerCommand command) {
        Test.print(command);
        switch (command) {
            case MOVE_FORWARD -> setDirect("w");
            case MOVE_BACKWARD -> setDirect("s");
            case MOVE_LEFT -> setDirect("a");
            case MOVE_RIGHT -> setDirect("d");

            case CANCEL_FORWARD -> cancelDirect("w");
            case CANCEL_BACKWARD -> cancelDirect("s");
            case CANCEL_LEFT -> cancelDirect("a");
            case CANCEL_RIGHT -> cancelDirect("d");

            case ATTACK -> {
                currentws = null;
                currentad = null;
                return "attack";
            }
        }

        if (currentws != null || currentad != null) return "movement";
        else return "rest";
    }

    private void setDirect(String direction) {
        if ("w".equals(direction) || "s".equals(direction)) {
            previousws = currentws;
            currentws = direction;
            this.newest = "ws";
        } else {
            previousad = currentad;
            currentad = direction;
            this.newest = "ad";
        }
    }

    private void cancelDirect(String cancelledDirection) {
        if ("w".equals(cancelledDirection) || "s".equals(cancelledDirection)) {
            if (cancelledDirection.equals(currentws)) {
                currentws = previousws;
                previousws = null;
                if (currentws == null) this.newest = "ad";
            } else if (previousws != null) {
                previousws = null;
            }
        } else {
            if (cancelledDirection.equals(currentad)) {
                currentad = previousad;
                previousad = null;
                if (currentad == null) this.newest = "ws";
            } else if (previousad != null) {
                previousad = null;
            }
        }
    }

    @Override
    public void loadTexture(String texturePath) {
        texturePath += "/";
        this.textureMap = new HashMap<>();
        textureMap.put("w", new Texture(texturePath + "moveforward"));
        textureMap.put("s", new Texture(texturePath + "movebackward"));
        textureMap.put("a", new Texture(texturePath + "moveleft"));
        textureMap.put("d", new Texture(texturePath + "moveright"));
    }

    @Override
    public Texture getTexture() {
        switch (newest) {
            case "ws" -> {return textureMap.get(currentws);}
            case "ad" -> {return textureMap.get(currentad);}
        }
        return null;
    }

    @Override
    public void apply() {
        if ("w".equals(currentws))
            entity.body.setVelocityY(entity.speed);
        else if ("s".equals(currentws))
            entity.body.setVelocityY(-entity.speed);
        else
            entity.body.setVelocityY(0);

        if ("a".equals(currentad))
            entity.body.setVelocityX(-entity.speed);
        else if ("d".equals(currentad))
            entity.body.setVelocityX(entity.speed);
        else
            entity.body.setVelocityX(0);
    }

    @Override
    public void tick() {
        getTexture().tick();
    }
}
