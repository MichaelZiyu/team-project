package game.interactor.gameModel.entityStates;

import java.util.HashSet;
import java.util.Set;

public class EntityStateMachine {

    private MovementState movementState;

    private final Set<StatusState> statusStateSet = new HashSet<>();


    public void tick() {
        movementState.tick();
        for (StatusState state : statusStateSet) {
            state.tick();
        }
    }
}
