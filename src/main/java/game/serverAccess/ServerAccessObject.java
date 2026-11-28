package game.serverAccess;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
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
        try {
            CompletableFuture<WebSocket> status = webSocket.sendText(data.toJson(), true);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Attempted Send failed", e);
        }
    }

    public void handleMessage(String data)
    {
        receiverBoundary.updateLocal(new ServerReceiverData(data));
        //WARNING: IT IS ENTIRELY POSSIBLE THAT THIS METHOD IS CALLED BEFORE THE PLAYER
        //HAS SENT ANY MESSAGE. IF SO, IT WILL BE AN EMPTY WORLDSTATE.
    }

    public void closeConnection(int statusCode, String reason)
    {
        webSocket.sendClose(statusCode,reason);
    }
}
