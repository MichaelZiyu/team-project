/*
a state in fsm
 */
public abstract class State {

    public abstract State transit(String input);

    public abstract String run();
}
