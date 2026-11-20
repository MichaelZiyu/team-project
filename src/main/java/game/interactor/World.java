package game.interactor;

import game.interactor.gameModel.scene.*;
import game.interactor.inputBoundaries.GameCommandBoundary;
import game.interactor.inputBoundaries.GameSnapshotBoundary;
import game.interactor.inputBoundaries.GameSynchronizationBoundary;
import game.interactor.inputBoundaries.GameTickBoundary;
import game.interactor.inputData.LocalPlayerCommand;
import game.interactor.inputData.RemotePlayerCommand;
import game.interactor.inputData.WorldState;
import game.interactor.outputData.RenderableData;

import java.util.List;

public class World implements GameCommandBoundary,
        GameSnapshotBoundary,
        GameSynchronizationBoundary,
        GameTickBoundary {

    private List<Scene> sceneList = List.of(
            new Filter(),
            new Entities(),
            new GameMap(),
            new Background()
    );

    @Override
    public void tick() {
        for (Scene scene : sceneList) {
            scene.tick(this);
        }
    }

    @Override
    public void synchronize(WorldState state) {

    }

    @Override
    public RenderableData getSnapshot() {
        return null;
    }

    @Override
    public void handleLocalPlayerCommand(LocalPlayerCommand command) {

    }

    @Override
    public void handleRemotePlayerCommand(RemotePlayerCommand command) {

    }


}
