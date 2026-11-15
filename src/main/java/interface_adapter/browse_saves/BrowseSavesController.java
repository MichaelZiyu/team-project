package interface_adapter.browse_saves;

import use_case.browse_saves.BrowseSavesInputBoundary;

/**
 * Interface-adapter controller for the Start Game action.
 * The action callback invokes this controller, which delegates to the
 * injected {@code BrowseSavesInputBoundary}.
 */

public final class BrowseSavesController {
    private final BrowseSavesInputBoundary inputBoundary;

    public BrowseSavesController(BrowseSavesInputBoundary inputBoundary) {
        this.inputBoundary = inputBoundary;
    }

    public void browseSaves() {
        inputBoundary.execute();
    }
}
