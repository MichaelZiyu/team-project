package game.serverAccess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import game.interactor.World;
import game.interactor.inputData.WorldState;

import java.util.Map;

class ServerReceiverData {
    //dummy impl
    private String data;

    ServerReceiverData(String message){
        data = message;
    }
    public WorldState deserialize() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, Object> receivedMap = mapper.readValue(data, new TypeReference<Map<String, Object>>() {
            });
            return new WorldState(receivedMap);
        } catch (Exception e) {
            throw new RuntimeException("Error in handling incoming message", e);
        }
    }
}

