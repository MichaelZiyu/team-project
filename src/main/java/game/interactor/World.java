package game.interactor;

import game.interactor.gameModel.entities.Player;
import game.interactor.gameModel.scene.*;
import game.interactor.inputBoundaries.*;
import game.interactor.inputData.LocalPlayerCommand;
import game.interactor.inputData.RemotePlayerCommand;
import game.interactor.inputData.WorldState;
import game.interactor.outputData.RenderableData;
import menu.inputData.InitializeGameData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class World implements GameCommandBoundary,
        GameSnapshotBoundary,
        GameSynchronizationBoundary,
        GameTickBoundary,
        InitializeWorldBoundary{

    private List<Scene> sceneList = List.of(
            new Filter(),
            new Entities(),
            new GameMap(),
            new Background()
    );

    private Player player;

    private Map<String, Player> playerRegistry = new HashMap<>();


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

    @Override
    public void initialize(InitializeGameData data) {

    }



}
