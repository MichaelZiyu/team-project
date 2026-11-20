package game.presenter;

import game.interactor.RenderableData;

/**
 * game renderer boundary that provides method to update the render data
 * for more information of rendering please see GameRenderer
 */
public interface GameRendererBoundary {

    public void submitSnapshot(RenderableData snapshot);
}
