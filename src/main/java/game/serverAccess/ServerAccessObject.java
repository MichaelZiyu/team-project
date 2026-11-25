package game.serverAccess;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;

public class ServerAccessObject implements ServerSenderBoundary {
    private HttpClient httpClient;
    private WebSocket.Builder builder;
    private WebSocket webSocket;
    private WebSocket.Listener webSocketListener;
    private ServerReceiverBoundary serverUseCaseInteractor;

    ServerAccessObject(URI uri, ServerReceiverBoundary serverUseCaseInteractor)
    {
        httpClient = HttpClient.newBuilder().build();
        builder = httpClient.newWebSocketBuilder();
        webSocketListener = new WebSocketListener(this);
        CompletableFuture<WebSocket> pendingWebSocket = builder.buildAsync(uri, webSocketListener);
        pendingWebSocket.whenComplete((completedWebSocket, exception) ->
        {
            if (exception != null)
            {
                System.out.println("conn failed");
            }
            else {
                webSocket = completedWebSocket;
            }
        });
        this.serverUseCaseInteractor = serverUseCaseInteractor;
    }

    @Override
    public void sendServer(ServerSenderData data) {
        //
    }

    public void handleMessage(String data)
    {
        serverUseCaseInteractor.updateLocal(convert(data));
    }

    private ServerReceiverData convert(String receivedMessage)
    {
        return new ServerReceiverData(receivedMessage);
    }
}
