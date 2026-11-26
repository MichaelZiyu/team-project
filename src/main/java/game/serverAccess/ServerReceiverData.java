package game.serverAccess;

class ServerReceiverData {
    //dummy impl
    private String data;

    ServerReceiverData(String message){
        this.data = message;
    }
    public String textFormatted()
    {
        return this.data;
    }
}

