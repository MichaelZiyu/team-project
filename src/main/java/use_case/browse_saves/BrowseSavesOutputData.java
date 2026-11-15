package use_case.browse_saves;

import java.util.List;
import use_case.save.SaveSummary;

/**
 * Application data returned by the Browse Saves interactor.
 * A successful read carries a list of {@code SaveSummary} values, which may
 * be empty. A catalog read failure produces the unavailable outcome.
 */

public sealed interface BrowseSavesOutputData {
    record Success(List<SaveSummary> saves) implements BrowseSavesOutputData {
        public Success {
            saves = List.copyOf(saves);
        }
    }

    record CatalogUnavailable() implements BrowseSavesOutputData {}
}
