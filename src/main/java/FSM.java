import java.util.Map;

public class FSM {

    private State state;

    public FSM(State initialState) {
        this.state = initialState;
    }

    public void run() {
        while (!(state instanceof ExitState)) {
            transit(state.run());
        }
    }

    private void transit(String input) {
        this.state = state.transit(input);
    }
}
