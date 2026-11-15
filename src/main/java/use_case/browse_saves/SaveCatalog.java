package use_case.browse_saves;

import java.util.List;
import use_case.save.SaveSummary;

/**
 * Data access interface owned by the Browse Saves use case.
 * Implementations return {@code SaveSummary} values and report read failures
 * through {@code ReadFailure}. The interactor depends on this contract,
 * which allows the application to supply a file catalog or a test substitute.
 */

public interface SaveCatalog {
    /** Returns the available saves, or an empty list when there are none. */
    List<SaveSummary> listSaves() throws ReadFailure;

    final class ReadFailure extends Exception {
        public ReadFailure(Throwable cause) {
            super(cause);
        }
    }
}
