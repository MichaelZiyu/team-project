package menu;

/** Input boundary for outer-engine menu commands. */
@FunctionalInterface
public interface UICommandBoundary {
    void execute(UICommand command, UIListener listener);
}
