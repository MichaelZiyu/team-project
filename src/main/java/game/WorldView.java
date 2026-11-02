package game;

import java.io.Serializable;

public class WorldView implements Serializable {
    private static final long serialVersionUID = 1L;

    enum Views {
        TOPDOWN,
        FRONT2D
    }

    public Views state;

    public WorldView() {
        this.state = Views.TOPDOWN;
    }

    public String toString() {
        if (state == Views.TOPDOWN) return "topdown";
        else if (state == Views.FRONT2D) return "front2d";
        else return null;
    }
}
