package states;

import game.gameModel.Entity;
import game.gameModel.Player;
import testpackage.Test;

import java.io.Serializable;
import java.util.*;

public class EntityStateManager implements Serializable {
    private static final long serialVersionUID = 1L;

    public final Map<StatusState, Integer> statusStates = new HashMap<>();
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
                "moveleft", new MoveLeftState(entity),
                "runleft", new RunLeftState(entity)
        ));
        statusStates.putAll(Map.of(
                //TODO status states
        ));
    }

    public void loadTexture(String texturePath) {
        for (StatusState state : statusStates.keySet()) {
            state.loadTexture(texturePath);
        }
        for (ActionState state : actionStates.values()) {
            state.loadTexture(texturePath);
        }
    }

    public void tick() {
        int tickCount;
        for (StatusState status : statusStates.keySet()) {
            tickCount = statusStates.get(status) - 1;
            if (tickCount > 0) {
                statusStates.put(status, tickCount);
                status.tick();
            }
            else {
                statusStates.put(status, 0);
                currentStatusStates.remove(status);
                status.resetTickCount();
            }
        }
        currentActionState.tick();
    }

    public void applyStatus() {
        for (StatusState status : currentStatusStates) {
            status.apply();
        }
    }

    public void handleCommand(Player.PlayerCommand command) {
        currentActionState = actionStates.get(currentActionState.handleCommand(command));
        Test.print(currentActionState);
    }





}
