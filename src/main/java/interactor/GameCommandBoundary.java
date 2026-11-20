package interactor;

public interface GameCommandBoundary {

    public void handleLocalPlayerCommand();

    public void handleServerPlayerCommand();
}
