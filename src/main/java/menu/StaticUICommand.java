package menu;

/** A menu command with no player-supplied value. */
public final class StaticUICommand extends UICommand {
    enum Action {
        OPEN_SETTINGS,
        BACK
    }

    public static final StaticUICommand OPEN_SETTINGS = new StaticUICommand(Action.OPEN_SETTINGS);
    public static final StaticUICommand BACK = new StaticUICommand(Action.BACK);

    private final Action action;

    private StaticUICommand(Action action) {
        this.action = action;
    }

    Action action() {
        return action;
    }
}
