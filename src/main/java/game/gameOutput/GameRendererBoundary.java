package game.gameOutput;

import game.interactor.outputData.RenderableData;

/**
 * game renderer boundary that provides method to update the render data
 * for more information of rendering please see GameRenderer
 */
public interface GameRendererBoundary {

    void submitSnapshot(RenderableData snapshot);
}
