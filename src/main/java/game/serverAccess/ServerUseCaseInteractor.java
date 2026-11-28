package game.serverAccess;

class ServerUseCaseInteractor implements ServerReceiverBoundary, ServerInputBoundary {
    private ServerReceiverData savedMessage;
    private ServerSenderBoundary senderBoundary;

    public ServerUseCaseInteractor()
    {
        savedMessage = new ServerReceiverData("");
    }
    public void initializeSenderBoundary(ServerSenderBoundary serverAccessObject)
    //note that it isn't immediately initialized
    {
        senderBoundary = serverAccessObject;
    }
    @Override
    public void updateLocal(ServerReceiverData data) {
        System.out.println("received message" + data.getData());
        savedMessage = data;
        //this message can be turned into a WorldState object through
    }
    public void sendMessage(ServerSenderData data)
    {
        senderBoundary.sendMessage(data);
    }

    @Override
    public void closeConnection(int statusCode, String reason) {
        senderBoundary.closeConnection(statusCode, reason);
    }
}
