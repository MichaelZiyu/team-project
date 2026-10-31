package game.gameModel;

import game.WorldView;

public class Item {

    private String name;
    private WorldView view;

    public Item(String name, WorldView view) {
        this.name = name;
        this.view = view;
    }

}
