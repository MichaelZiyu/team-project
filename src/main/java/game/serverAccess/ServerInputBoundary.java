package game.serverAccess;

public interface ServerInputBoundary {
    void sendMessage(ServerSenderData data);
    void closeConnection(int statusCode, String reason);
}
