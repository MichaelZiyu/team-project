package game.gameModel;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class Texture {

    private WorldView view;
    private Map<WorldView.ViewState, JLabel> imageIndex;

    public Texture(WorldView view, Map<WorldView.ViewState, String> filepathIndex) {
        this.view = view;
        this.imageIndex = new HashMap<>();
        for (WorldView.ViewState state : WorldView.ViewState.values()) {
            imageIndex.put(state, new JLabel(new ImageIcon(filepathIndex.get(state))));
        }
    }

    public JLabel getTexture() {
        return imageIndex.get(view.state);
    }

}
