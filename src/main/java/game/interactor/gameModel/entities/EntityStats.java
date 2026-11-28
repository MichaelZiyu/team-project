package game.interactor.gameModel.entities;

/**
 * data structure representing statistics of any entity such as hp, attack...
 */
public abstract class EntityStats {
    private float[] location;
    private String id;

    public void setLocation(float[] newLocation)
    {
        location = newLocation;
    }
    public void setId(String newID)
    {
        id = newID;
    }

    public float[] getLocation() {
        return location;
    }

    public String getId()
    {
        return id;
    }
}
