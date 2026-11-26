package game.serverAccess;

import java.net.URI;

public class ServerAccessBuilder {
    private ServerUseCaseInteractor interactor;
    private ServerAccessObject object;

    public ServerUseCaseInteractor build(String publicIP, String port)
    {
        URI uri = URI.create("ws://"+publicIP+":"+port);

        ServerAccessObject newObject = new ServerAccessObject();
        ServerUseCaseInteractor newInteractor = new ServerUseCaseInteractor();
        newObject.initializeReceiverBoundary(newInteractor);
        newInteractor.initializeSenderBoundary(newObject);
        newObject.connectWebsocket(uri);

        interactor = newInteractor;
        object = newObject;

        return interactor;
    }

    public ServerUseCaseInteractor getInteractor() {
        return interactor;
    }
}
