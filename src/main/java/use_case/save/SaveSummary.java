package use_case.save;

import java.util.Objects;

/**
 * Application data describing one entry in the save catalog.
 * It pairs a {@code SaveId} used for selection with a display name used
 * by the presenter.
 * The information needed to list a save before opening it.
 */

public final class SaveSummary {
    private final SaveId id;
    private final String displayName;

    public SaveSummary(SaveId id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public SaveId id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SaveSummary)) {
            return false;
        }
        SaveSummary otherSummary = (SaveSummary) other;
        return Objects.equals(id, otherSummary.id)
                && Objects.equals(displayName, otherSummary.displayName);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hashCode(id) + Objects.hashCode(displayName);
    }

    @Override
    public String toString() {
        return "SaveSummary[id=" + id + ", displayName=" + displayName + "]";
    }
}
