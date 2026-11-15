package use_case.browse_saves;

/**
 * Output boundary owned by the Browse Saves use case.
 * The interactor passes {@code BrowseSavesOutputData} through this interface.
 * The presenter implements the interface, so the interactor can report
 * results through the use-case contract.
 */

public interface BrowseSavesOutputBoundary {
    void present(BrowseSavesOutputData result);
}
