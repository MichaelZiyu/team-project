package states.EntityStates;

import game.gameModel.Entity;
import game.gameModel.Player;
import jswing.Texture;
import states.EntityStates.ActionStates.*;
import states.EntityStates.StatusStates.StatusState;

import java.io.Serializable;
import java.util.*;

public class EntityStateManager implements Serializable {
    private static final long serialVersionUID = 1L;

    public final Map<String, StatusState> statusStates = new HashMap<>();
    public final Map<StatusState, Integer> statusStateCounter = new HashMap<>();
    public final Set<StatusState> currentStatusStates = new HashSet<>();

    public final Map<String, ActionState> actionStates = new HashMap<>();
    public final List<ActionState> currentActionStates = new ArrayList<>();

    private Entity entity;

    public EntityStateManager(Entity entity) {
        this.entity = entity;
        register();
        this.currentActionStates.add(actionStates.get("rest"));
    }

    private void register() {
        actionStates.putAll(Map.of(
                "rest", new RestState(entity),
                "moveleft", new MoveLeftState(entity),
                "moveforward", new MoveForwardState(entity),
                "moveright", new MoveRightState(entity),
                "movebackward", new MoveBackwardState(entity),
                "runleft", new RunLeftState(entity)
        ));
        statusStates.putAll(Map.of(
                //TODO status states
        ));
    }

    public void loadTexture(String texturePath) {
        for (StatusState state : statusStateCounter.keySet()) {
            state.loadTexture(texturePath);
        }
        for (ActionState state : actionStates.values()) {
            state.loadTexture(texturePath);
        }
    }

    public void tick() {
        int tickCount;
        for (StatusState status : statusStateCounter.keySet()) {
            tickCount = statusStateCounter.get(status) - 1;
            if (tickCount > 0) {
                statusStateCounter.put(status, tickCount);
                status.tick();
            }
            else {
                statusStateCounter.put(status, 0);
                currentStatusStates.remove(status);
                status.resetTickCount();
            }
        }
        for (ActionState state : currentActionStates) {
            state.tick();
        }
    }

    public void apply() {
//        for (ActionState state : currentActionStates) {
//            state.apply();
//        }
        for (StatusState status : currentStatusStates) {
            status.apply();
        }
    }

    public void handleCommand(Player.PlayerCommand command) {
        ActionState nextState = actionStates.get(currentActionStates.get(0).handleCommand(command));
        if (nextState != currentActionStates.get(0)) currentActionStates.get(0).resetTickCount();
        currentActionStates.set(0, nextState);
    }

//    public Texture getTexture() {
//
//    }





}
