package game.serverAccess;

class ServerUseCaseInteractor implements ServerReceiverBoundary, ServerInputBoundary {
    private ServerReceiverData prevMessage;
    private ServerSenderBoundary senderBoundary;

    public ServerUseCaseInteractor()
    {
        prevMessage = new ServerReceiverData("");
    }
    public void initializeSenderBoundary(ServerSenderBoundary serverAccessObject)
    //note that it isn't immediately initialized because
    //it is ok if we establish a connection w/ server first, before spinning up use case interactor
    {
        senderBoundary = serverAccessObject;
    }
    @Override
    public void updateLocal(ServerReceiverData data) {
        //TODO
        System.out.println("received: " + data.textFormatted());
        prevMessage = data;
    }
    public void sendMessage(ServerSenderData data)
    {
        //TODO
        senderBoundary.sendMessage(data);
    }

    @Override
    public void closeConnection(int statusCode, String reason) {
        senderBoundary.closeConnection(statusCode, reason);
    }
}
