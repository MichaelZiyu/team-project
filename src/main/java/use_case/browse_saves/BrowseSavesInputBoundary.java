package use_case.browse_saves;

/**
 * Input boundary for the Browse Saves use case.
 * The controller calls its operation when the player selects
 * Start Game. The interactor implements this contract.
 */

public interface BrowseSavesInputBoundary {
    void execute();
}
