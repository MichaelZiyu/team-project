package game.serverAccess;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;

public class ServerAccessObject implements ServerSenderBoundary {
    //Lots of system prints, i am not handling any errors for this draft

    private HttpClient httpClient;
    private WebSocket.Builder builder;
    private WebSocket webSocket;
    private WebSocket.Listener webSocketListener;
    private ServerReceiverBoundary serverUseCaseInteractor;
    private volatile boolean connected;

    ServerAccessObject(URI uri, ServerReceiverBoundary serverReceiverBoundary)
    {
        connected = false;
        serverUseCaseInteractor = serverReceiverBoundary;
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
                connected = true;
            }
        });
    }

    @Override
    public void sendMessage(ServerSenderData data) {
        //i do not handle the case where it doesn't successfully send BUT we are connected
        if (connected) {
            CompletableFuture<WebSocket> status = webSocket.sendText(convertSend(data), true);
        }
        else{
            System.out.println("not connected");
        }
    }

    public void handleMessage(String data)
    {
        serverUseCaseInteractor.updateLocal(convertReceived(data));
    }

    private ServerReceiverData convertReceived(String receivedMessage)
    {
        return new ServerReceiverData(receivedMessage);
    }

    private String convertSend(ServerSenderData data)
    {
        return data.textFormatted();
    }
}
