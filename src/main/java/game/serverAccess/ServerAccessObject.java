package game.serverAccess;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;

class ServerAccessObject implements ServerSenderBoundary {
    //Lots of system prints, i am not handling any errors for this draft

    private HttpClient httpClient;
    private WebSocket.Builder builder;
    private WebSocket webSocket;
    private WebSocket.Listener webSocketListener;
    private ServerReceiverBoundary receiverBoundary;

    ServerAccessObject()
    {
        httpClient = HttpClient.newBuilder().build();
        builder = httpClient.newWebSocketBuilder();
        webSocketListener = new WebSocketListener(this);
    }

    public void initializeReceiverBoundary(ServerReceiverBoundary serverUseCaseInteractor)
    {
        receiverBoundary = serverUseCaseInteractor;
    }

    public void connectWebsocket(URI uri)
    {
        CompletableFuture<WebSocket> pendingWebSocket = builder.buildAsync(uri, webSocketListener);
        try {
            webSocket = pendingWebSocket.join();
        }
        catch (Exception e)
        {
            throw new RuntimeException("WebSocket connection failed", e);
        }
    }

    @Override
    public void sendMessage(ServerSenderData data) {
        //i do not handle the case where it doesn't successfully send BUT we are connected
        CompletableFuture<WebSocket> status = webSocket.sendText(convertSend(data), true);
    }

    public void handleMessage(String data)
    {
        receiverBoundary.updateLocal(convertReceived(data));
    }

    private ServerReceiverData convertReceived(String receivedMessage)
    {
        //TODO
        return new ServerReceiverData(receivedMessage);
    }

    private String convertSend(ServerSenderData data)
    {
        //TODO
        return data.textFormatted();
    }

    public void closeConnection(int statusCode, String reason)
    {
        webSocket.sendClose(statusCode,reason);
    }
}
