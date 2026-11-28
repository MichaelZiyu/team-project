package game.serverAccess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import game.interactor.inputData.WorldState;

public class ServerSenderData {
    private WorldState worldState;

    public ServerSenderData(WorldState state){
        worldState = state;
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(worldState.getState());
    }
}
