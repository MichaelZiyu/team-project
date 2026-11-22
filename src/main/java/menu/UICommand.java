package menu;

/** A command submitted by an outer-engine view. */
public abstract sealed class UICommand permits StaticUICommand, DynamicUICommand {
}
