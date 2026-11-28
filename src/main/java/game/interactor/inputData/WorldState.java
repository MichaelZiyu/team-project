package game.interactor.inputData;

import game.interactor.gameModel.entities.Entity;
import game.interactor.gameModel.entities.EntityObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class WorldState {

    private Map<String, Object> stateMap;
    public WorldState(Set<? extends EntityObject> objects)
    {
        Map<String, Object> newMap = new HashMap<>();
        for(EntityObject object: objects)
        {
            newMap.put(object.getID(), object.getLocation());
        }
        this.stateMap = newMap;
    }

    public Map<String, Object> getState(){
        return stateMap;
    }

}
