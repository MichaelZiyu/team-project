package game.serverAccess;

import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

public class WebSocketListener implements WebSocket.Listener{
    private ServerAccessObject serverAccessObject;
    private StringBuilder message;
    WebSocketListener(ServerAccessObject serverAccessObject){
        this.serverAccessObject = serverAccessObject;
        this.message = new StringBuilder();
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        message.append(data);
        if(last)
        {
            String finishedMessage = message.toString();
            serverAccessObject.handleMessage(finishedMessage);
            message.setLength(0);
        }
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }
}
