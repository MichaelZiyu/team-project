package game;

import game.gameModel.Renderable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class GameMap implements Iterable<Renderable>{

    public GameMap(String seed) {

    }

    @Override
    public Iterator<Renderable> iterator() {
        return Collections.emptyIterator();
    }
}
