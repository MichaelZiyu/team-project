package interface_adapter.browse_saves;

import interface_adapter.navigation.NavigationViewModel;
import interface_adapter.navigation.ScreenId;
import interface_adapter.save_list.SaveListViewModel;
import interface_adapter.start_menu.StartMenuViewModel;
import java.util.List;
import use_case.browse_saves.BrowseSavesOutputBoundary;
import use_case.browse_saves.BrowseSavesOutputData;
import use_case.save.SaveSummary;

/**
 * Interface-adapter presenter for Browse Saves.
 * It implements {@code BrowseSavesOutputBoundary} and converts application
 * results into title messages, save-list options, and navigation requests.
 * It writes to the start-menu, save-list, and navigation view models.
 */

public final class BrowseSavesPresenter implements BrowseSavesOutputBoundary {
    private final StartMenuViewModel startMenu;
    private final SaveListViewModel saveList;
    private final NavigationViewModel navigation;

    public BrowseSavesPresenter(
            StartMenuViewModel startMenu, SaveListViewModel saveList, NavigationViewModel navigation) {
        this.startMenu = startMenu;
        this.saveList = saveList;
        this.navigation = navigation;
    }

    @Override
    public void present(BrowseSavesOutputData result) {
        switch (result) {
            case BrowseSavesOutputData.Success success -> showPlayOptions(success.saves());
            case BrowseSavesOutputData.CatalogUnavailable unavailable ->
                    startMenu.setMessage("Could not read saved worlds.");
        }
    }

    private void showPlayOptions(List<SaveSummary> saves) {
        startMenu.setMessage("");
        saveList.setOptions(saves.stream()
                .map(save -> new SaveListViewModel.Option(save.id().value(), save.displayName()))
                .toList());
        navigation.requestScreen(saves.isEmpty() ? ScreenId.NEW_WORLD : ScreenId.SAVES);
    }
}
