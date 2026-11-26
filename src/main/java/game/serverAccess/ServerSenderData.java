package game.serverAccess;

public class ServerSenderData {
    //dummy impl
    private String data;

    public ServerSenderData(String message){
        this.data = message;
    }

    public String textFormatted()
    {
        return this.data;
    }
}
