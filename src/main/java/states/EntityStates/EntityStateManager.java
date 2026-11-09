package states.EntityStates;

import game.gameModel.Entity;
import game.gameModel.Player;
import jswing.Texture;
import states.EntityStates.ActionStates.*;
import states.EntityStates.StatusStates.StatusState;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the current states(including action states and status states) of an entity and the transition among states.
 */
public class EntityStateManager implements Serializable {
    private static final long serialVersionUID = 1L;

    /**status states*/
    public final Map<String, StatusState> statusStates = new HashMap<>();
    public final Set<StatusState> currentStatusStates = new HashSet<>();
    /** Tracks the remaining duration(in ticks) of each status state*/
    public final Map<StatusState, Integer> statusStateCounter = new HashMap<>();

    /**action states*/
    public final Map<String, ActionState> actionStates = new HashMap<>();
    public ActionState currentActionState;

    private Entity entity;

    /**
     * register and initialize all entity states, and set default current state to RestState
     *
     * @param entity the entity whose states are managed by this state manager
     */
    public EntityStateManager(Entity entity) {
        this.entity = entity;
        register();
        this.currentActionState = actionStates.get("rest");
    }

    /**
     * initialize the maps from state names to state objects.
     */
    private void register() {
        actionStates.putAll(Map.of(
                "rest", new RestState(entity),
                "moving", new MovingState(entity)
        ));
        statusStates.putAll(Map.of(
                //TODO status states
        ));
    }

    /** load texture of all states*/
    public void loadTexture(String texturePath) {
        for (StatusState state : statusStateCounter.keySet()) {
            state.loadTexture(texturePath);
        }
        for (ActionState state : actionStates.values()) {
            state.loadTexture(texturePath);
        }
    }

    /**
     * ticks all entity states. maintains current status state by checking if their durations are zero
     */
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

    /** applies the effect from all entity states the entity has right now*/
    public void apply() {
        currentActionState.apply();
        for (StatusState status : currentStatusStates) {
            status.apply();
        }
    }

    /** handles player command and do the transition of action states
     * let action states handle additional commands from previous transition if any
     */
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

    /** return the current action state texture*/
    public Texture getTexture() {
        return currentActionState.getTexture();
    }

    public String toString() {
        String statuses = currentStatusStates.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        return "ActionState: " + currentActionState + "\nStatusState: " + statuses;
    }





}
