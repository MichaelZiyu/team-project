package states;

import java.io.Serializable;
import java.util.*;

public class EntityStateManager implements Serializable {
    private static final long serialVersionUID = 1L;

    public EntityState rest = new EntityState("rest");
    public EntityState walking = new EntityState("walking");

    public List<EntityState> entityStates = new ArrayList<>();
    {
        entityStates.add(rest);
        entityStates.add(walking);
    }

}
