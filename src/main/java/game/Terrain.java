package game;

import java.io.Serializable;

public class Terrain implements Serializable {
    private static final long serialVersionUID = 1L;

    private String seed;

    public Terrain(String seed) {
        this.seed = seed;
    }
}
