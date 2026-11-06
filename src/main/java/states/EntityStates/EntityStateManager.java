package states.EntityStates;

import game.gameModel.Entity;
import game.gameModel.Player;
import jswing.Texture;
import states.EntityStates.ActionStates.*;
import states.EntityStates.StatusStates.StatusState;
import testpackage.Test;

import java.io.Serializable;
import java.util.*;

public class EntityStateManager implements Serializable {
    private static final long serialVersionUID = 1L;

    public final Map<String, StatusState> statusStates = new HashMap<>();
    public final Map<StatusState, Integer> statusStateCounter = new HashMap<>();
    public final Set<StatusState> currentStatusStates = new HashSet<>();

    public final Map<String, ActionState> actionStates = new HashMap<>();
    public ActionState currentActionState;

    private Entity entity;

    public EntityStateManager(Entity entity) {
        this.entity = entity;
        register();
        this.currentActionState = actionStates.get("rest");
    }

    private void register() {
        actionStates.putAll(Map.of(
                "rest", new RestState(entity),
                "movement", new MovementState(entity)
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
        currentActionState.tick();
    }

    public void apply() {
        currentActionState.apply();
        for (StatusState status : currentStatusStates) {
            status.apply();
        }
    }

    public void handleCommand(Player.PlayerCommand command) {
        String[] transition = currentActionState.handleCommand(command).split("-", 2);

        ActionState nextState = actionStates.get(transition[0]);

        if (nextState != currentActionState) {
            currentActionState.resetTickCount();
            currentActionState = nextState;
        }

        if (transition.length > 1) {
            nextState.handleCommand(Player.PlayerCommand.valueOf(transition[1]));
        }
    }

    public Texture getTexture() {
        return currentActionState.getTexture();
    }





}
