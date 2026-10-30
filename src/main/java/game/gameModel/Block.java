package game.gameModel;

import java.util.Map;

public class Block extends Item {

    public Block(String name, WorldView view, Map<WorldView.ViewState, String> filepathIndex) {
        super(name, view, filepathIndex);
    }
}
