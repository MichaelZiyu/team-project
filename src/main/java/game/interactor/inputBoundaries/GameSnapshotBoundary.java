package game.interactor.inputBoundaries;

import game.interactor.outputData.RenderableData;

/**
 * provide a method to create a snapshot for renderer
 */
public interface GameSnapshotBoundary {

    public RenderableData getSnapshot();
}
