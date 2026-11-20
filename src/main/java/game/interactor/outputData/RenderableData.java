package game.interactor.outputData;

import game.interactor.gameModel.Texture;

import java.util.*;

/**
 * data structure used for renderer to render
 */
public class RenderableData implements Iterable<RenderableData.RenderableItem> {

    private final Map<Texture, Set<int[]>> map;

    public RenderableData(Map<Texture, Set<int[]>> map) {
        this.map = map;
    }

    public record RenderableItem(Texture texture, int x, int y) {}

    @Override
    public Iterator<RenderableItem> iterator() {
        Iterator<Map.Entry<Texture, Set<int[]>>> mapIterator =
                map.entrySet().iterator();

        return new Iterator<>() {
            private Texture currentTexture;
            private Iterator<int[]> positionIterator =
                    Collections.emptyIterator();

            @Override
            public boolean hasNext() {
                while (!positionIterator.hasNext() && mapIterator.hasNext()) {
                    Map.Entry<Texture, Set<int[]>> entry = mapIterator.next();
                    currentTexture = entry.getKey();
                    positionIterator = entry.getValue().iterator();
                }
                return positionIterator.hasNext();
            }

            @Override
            public RenderableItem next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                int[] position = positionIterator.next();
                return new RenderableItem(
                        currentTexture,
                        position[0],
                        position[1]
                );
            }
        };
    }
}
