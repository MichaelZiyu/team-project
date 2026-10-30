package game.gameModel;

import java.util.Map;

public class Item {

    private String name;
    private WorldView view;
    private Texture texture;

    public Item(String name, WorldView view, Map<WorldView.ViewState, String> filepathIndex) {
        this.name = name;
        this.view = view;
        this.texture = new Texture(view, filepathIndex);
        // if an item's texture does not related to the world view, then set filepath for different views to the same.
    }

}
