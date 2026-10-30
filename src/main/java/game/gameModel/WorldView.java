package game.gameModel;

public class WorldView {

    enum ViewState {
        TOPDOWN,
        FRONT2D
    }

    public ViewState state;

    public WorldView() {
        this.state = ViewState.TOPDOWN;
    }
}
