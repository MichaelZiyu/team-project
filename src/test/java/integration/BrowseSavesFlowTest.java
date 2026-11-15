package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import file.LocalSaveStorage;
import interface_adapter.browse_saves.BrowseSavesController;
import interface_adapter.browse_saves.BrowseSavesPresenter;
import interface_adapter.navigation.NavigationViewModel;
import interface_adapter.navigation.ScreenId;
import interface_adapter.save_list.SaveListViewModel;
import interface_adapter.start_menu.StartMenuViewModel;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import use_case.browse_saves.BrowseSavesInteractor;
import use_case.save.SaveId;

class BrowseSavesFlowTest {
    @TempDir
    Path tempDirectory;

    private final List<ScreenId> requestedScreens = new ArrayList<>();
    private List<SaveListViewModel.Option> options = List.of();
    private String titleMessage = "";

    @Test
    void readsFreshSavesOnEachStartAndLoadsTheSelectedLegacyFile() throws Exception {
        Path saveDirectory = tempDirectory.resolve("saves");
        LocalSaveStorage saveStorage = new LocalSaveStorage(saveDirectory);
        BrowseSavesController controller = createController(saveStorage);

        controller.browseSaves();
        assertEquals(List.of(ScreenId.TITLE, ScreenId.NEW_WORLD), requestedScreens);
        assertTrue(options.isEmpty());

        Files.createDirectory(saveDirectory);
        Path saveFile = saveDirectory.resolve("first.world.dat");
        try (InputStream sample = getClass().getResourceAsStream("/saves/sample.dat")) {
            assertNotNull(sample);
            Files.copy(sample, saveFile);
        }
        Files.writeString(saveDirectory.resolve("notes.txt"), "Ignored by the save catalog");
        controller.browseSaves();

        assertEquals(List.of(new SaveListViewModel.Option("first.world", "first.world")), options);
        assertEquals(List.of(ScreenId.TITLE, ScreenId.NEW_WORLD, ScreenId.SAVES), requestedScreens);
        assertNotNull(saveStorage.loadSave(new SaveId(options.getFirst().selectionKey())).getPlayer());

        Files.delete(saveFile);
        assertThrows(NoSuchFileException.class, () -> saveStorage.loadSave(new SaveId("first.world")));
        controller.browseSaves();
        assertTrue(options.isEmpty());
        assertEquals(List.of(ScreenId.TITLE, ScreenId.NEW_WORLD, ScreenId.SAVES, ScreenId.NEW_WORLD),
                requestedScreens);
    }

    @Test
    void displaysCatalogFailureOnTheTitleAndClearsItAfterRetry() throws Exception {
        Path saveDirectory = Files.createFile(tempDirectory.resolve("saves"));
        BrowseSavesController controller = createController(new LocalSaveStorage(saveDirectory));

        controller.browseSaves();
        assertEquals(List.of(ScreenId.TITLE), requestedScreens);
        assertEquals("Could not read saved worlds.", titleMessage);

        Files.delete(saveDirectory);
        controller.browseSaves();
        assertEquals(List.of(ScreenId.TITLE, ScreenId.NEW_WORLD), requestedScreens);
        assertEquals("", titleMessage);

        controller.browseSaves();
        assertEquals(List.of(ScreenId.TITLE, ScreenId.NEW_WORLD, ScreenId.NEW_WORLD), requestedScreens);
    }

    private BrowseSavesController createController(LocalSaveStorage saveStorage) {
        StartMenuViewModel startMenu = new StartMenuViewModel();
        SaveListViewModel saveList = new SaveListViewModel();
        NavigationViewModel navigation = new NavigationViewModel();
        startMenu.addMessageListener(message -> titleMessage = message);
        saveList.addOptionsListener(saveOptions -> options = saveOptions);
        navigation.addScreenListener(requestedScreens::add);

        BrowseSavesPresenter presenter = new BrowseSavesPresenter(startMenu, saveList, navigation);
        return new BrowseSavesController(new BrowseSavesInteractor(saveStorage, presenter));
    }
}
