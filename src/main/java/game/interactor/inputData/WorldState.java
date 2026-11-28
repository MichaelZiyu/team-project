package game.interactor.inputData;

import game.interactor.gameModel.entities.Entity;
import game.interactor.gameModel.entities.EntityObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class WorldState {

    private Map<String, Object> stateMap;
    public WorldState(Set<? extends EntityObject> objects)
            //for sending
    {
        Map<String, Object> newMap = new HashMap<>();
        for(EntityObject object: objects)
        {
            newMap.put(object.getStats().getId(), object.getStats().getLocation());
        }
        this.stateMap = newMap;
    }
    public WorldState(Map<String, Object> pendingMap)
            //for receiving
    {
        this.stateMap = new HashMap<>(pendingMap);
    }

    public Map<String, Object> getState(){
        return stateMap;
    }

}
