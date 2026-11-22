package entity.settings;

import java.util.Objects;

/** An immutable snapshot of the configurable game settings. */
public final class GameSettings {
    private final FrameSize frame_size;

    public GameSettings(FrameSize frame_size) {
        this.frame_size = Objects.requireNonNull(frame_size);
    }

    public FrameSize frameSize() {
        return frame_size;
    }

    public GameSettings withFrameSize(FrameSize frame_size) {
        return new GameSettings(frame_size);
    }

    public record FrameSize(int width, int height) {
        @Override
        public String toString() {
            return width + " × " + height;
        }
    }
}
