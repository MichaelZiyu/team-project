package states;

import game.gameModel.Entity;
import game.gameModel.Player;

import java.io.Serializable;
import java.util.*;

public class EntityStateManager implements Serializable {
    private static final long serialVersionUID = 1L;

    public final Map<StatusState, Integer> statusStates = new HashMap<>();
    public final Set<StatusState> currentStatusStates = new HashSet<>();

    public final Set<ActionState> actionStates = new HashSet<>();
    public ActionState currentActionState;

    private Entity entity;

    public EntityStateManager(Entity entity) {
        this.entity = entity;
        this.currentActionState = new RestState(entity);
        register();
    }

    private void register() {
        actionStates.addAll(Set.of(
                new RestState(entity),
                new RunningState(entity)
        ));
        statusStates.putAll(Map.of(
                //TODO status states
        ));
    }

    public void loadTexture(String texturePath) {
        for (StatusState state : statusStates.keySet()) {
            state.loadTexture(texturePath);
        }
        currentActionState.loadTexture(texturePath);
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
        currentActionState = currentActionState.handleCommand(command);
    }





}
