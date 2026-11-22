package menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import entity.settings.GameSettings;
import entity.settings.GameSettings.FrameSize;
import java.util.ArrayList;
import java.util.List;
import menu.SettingsResult.Outcome;
import org.junit.jupiter.api.Test;

class SettingsCommandHandlerTest {
    private static final FrameSize STANDARD = new FrameSize(800, 600);
    private static final FrameSize WIDE = new FrameSize(1600, 900);

    private final TestSettingsAccess access = new TestSettingsAccess();
    private final UICommandBoundary commands = new SettingsCommandHandler(access, List.of(STANDARD, WIDE));
    private final List<SettingsResult> results = new ArrayList<>();

    @Test
    void openingAndReturningReportTheAppliedSettingsWithoutResizing() {
        commands.execute(StaticUICommand.OPEN_SETTINGS, results::add);
        assertResult(Outcome.OPENED, STANDARD);

        commands.execute(StaticUICommand.BACK, results::add);
        assertResult(Outcome.BACK, STANDARD);
        assertEquals(0, access.applyCalls);
    }

    @Test
    void applyingEitherPresetReportsTheSizeAcceptedByTheDisplay() {
        commands.execute(new DynamicUICommand(new GameSettings(WIDE)), results::add);
        assertResult(Outcome.APPLIED, WIDE);

        commands.execute(StaticUICommand.OPEN_SETTINGS, results::add);
        assertResult(Outcome.OPENED, WIDE);

        commands.execute(new DynamicUICommand(new GameSettings(STANDARD)), results::add);
        assertResult(Outcome.APPLIED, STANDARD);
        assertEquals(2, access.applyCalls);
    }

    @Test
    void rejectingAnUnsupportedSizeOrFailingToResizeReportsTheAppliedSize() {
        commands.execute(new DynamicUICommand(new GameSettings(new FrameSize(1600, 600))), results::add);
        assertResult(Outcome.UNSUPPORTED_FRAME_SIZE, STANDARD);
        assertEquals(0, access.applyCalls);

        access.resizeFails = true;
        commands.execute(new DynamicUICommand(new GameSettings(WIDE)), results::add);
        assertResult(Outcome.APPLY_FAILED, STANDARD);
        assertEquals(1, access.applyCalls);
    }

    private void assertResult(Outcome outcome, FrameSize frame_size) {
        SettingsResult result = results.getLast();
        assertEquals(outcome, result.outcome());
        assertEquals(frame_size, result.appliedSettings().frameSize());
    }

    private static final class TestSettingsAccess implements GameSettingsAccess {
        private GameSettings appliedSettings = new GameSettings(STANDARD);
        private int applyCalls;
        private boolean resizeFails;

        @Override
        public GameSettings read() {
            return appliedSettings;
        }

        @Override
        public void apply(GameSettings requestedSettings) {
            applyCalls++;
            if (resizeFails) {
                throw new IllegalStateException("Simulated resize failure");
            }
            appliedSettings = requestedSettings;
        }
    }
}
