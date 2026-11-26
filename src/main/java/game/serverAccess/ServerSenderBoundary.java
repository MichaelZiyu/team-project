package game.serverAccess;

public interface ServerSenderBoundary {
    void sendMessage(ServerSenderData data);
    void closeConnection(int statusCode, String reason);
}
