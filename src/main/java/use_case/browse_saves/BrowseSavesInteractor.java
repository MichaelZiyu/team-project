package use_case.browse_saves;

/**
 * Use-case interactor that reads the save catalog on each request.
 * It queries the injected {@code SaveCatalog} and sends a
 * {@code BrowseSavesOutputData} result through the injected output boundary.
 * Its dependencies are on application interfaces and data types.
 */

public final class BrowseSavesInteractor implements BrowseSavesInputBoundary {
    private final SaveCatalog saveCatalog;
    private final BrowseSavesOutputBoundary outputBoundary;

    public BrowseSavesInteractor(SaveCatalog saveCatalog, BrowseSavesOutputBoundary outputBoundary) {
        this.saveCatalog = saveCatalog;
        this.outputBoundary = outputBoundary;
    }

    @Override
    public void execute() {
        BrowseSavesOutputData result;
        try {
            result = new BrowseSavesOutputData.Success(saveCatalog.listSaves());
        } catch (SaveCatalog.ReadFailure failure) {
            result = new BrowseSavesOutputData.CatalogUnavailable();
        }
        outputBoundary.present(result);
    }
}
