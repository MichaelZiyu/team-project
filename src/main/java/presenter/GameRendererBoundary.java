package presenter;

import interactor.RenderableData;

public interface GameRendererBoundary {

    public void submitSnapshot(RenderableData snapshot);
}
