package use_case.save;

import java.util.Objects;

/**
 * Application value object representing a save's selection key.
 * It stores a nonempty string. The storage adapter interprets the key
 * when resolving a saved world.
 * Identifies a save independently of its display name.
 */

public final class SaveId {
    private final String value;

    public SaveId(String value) {
        Objects.requireNonNull(value, "value");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Save ID must have a value");
        }
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SaveId)) {
            return false;
        }
        SaveId otherId = (SaveId) other;
        return value.equals(otherId.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "SaveId[value=" + value + "]";
    }
}
