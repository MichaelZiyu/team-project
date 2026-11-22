import entity.settings.GameSettings;
import entity.settings.GameSettings.FrameSize;
import file.FileManager;
import file.LocalSaveStorage;
import interface_adapter.browse_saves.BrowseSavesController;
import interface_adapter.browse_saves.BrowseSavesPresenter;
import interface_adapter.navigation.NavigationViewModel;
import interface_adapter.save_list.SaveListViewModel;
import interface_adapter.start_menu.StartMenuViewModel;
import java.util.List;
import javax.swing.SwingUtilities;
import jswing.PanelManager;
import jswing.SettingMenuPanel;
import jswing.StartMenuPanel;
import jswing.WindowManager;
import menu.GameSettingsAccess;
import menu.SettingsCommandHandler;
import menu.UICommandBoundary;
import states.PanelStateController;
import use_case.browse_saves.BrowseSavesInteractor;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::start);
    }

    /**
     * Constructs and connects the save catalog, interactor, presenter,
     * controller, view models, and Swing panels. Supplies the concrete implementations
     * and starts the UI on the Swing event thread.
     */

    private static void start() {
        LocalSaveStorage saveStorage = new LocalSaveStorage(FileManager.SAVE_DIRECTORY);
        StartMenuViewModel startMenuModel = new StartMenuViewModel();
        SaveListViewModel saveListModel = new SaveListViewModel();
        NavigationViewModel navigation = new NavigationViewModel();

        BrowseSavesPresenter presenter = new BrowseSavesPresenter(startMenuModel, saveListModel, navigation);
        BrowseSavesInteractor interactor = new BrowseSavesInteractor(saveStorage, presenter);
        BrowseSavesController controller = new BrowseSavesController(interactor);

        List<FrameSize> frameSizes = List.of(
                new FrameSize(WindowManager.MIN_FRAME_WIDTH, WindowManager.MIN_FRAME_HEIGHT),
                new FrameSize(WindowManager.MAX_FRAME_WIDTH, WindowManager.MAX_FRAME_HEIGHT));
        GameSettingsAccess settingsAccess = new GameSettingsAccess() {
            @Override
            public GameSettings read() {
                return new GameSettings(new FrameSize(WindowManager.FRAME_WIDTH, WindowManager.FRAME_HEIGHT));
            }

            @Override
            public void apply(GameSettings requestedSettings) {
                FrameSize frame_size = requestedSettings.frameSize();
                PanelStateController.setWindowSize(frame_size.width(), frame_size.height());
            }
        };
        UICommandBoundary settingsCommands = new SettingsCommandHandler(settingsAccess, frameSizes);
        SettingMenuPanel settingMenu = new SettingMenuPanel(
                settingsCommands, frameSizes,
                () -> PanelStateController.transition("settings"),
                () -> PanelStateController.transition("Exit"));

        StartMenuPanel startMenu = new StartMenuPanel(
                startMenuModel,
                controller::browseSaves,
                settingMenu::openSettings,
                () -> PanelStateController.transition("Exit"));
        PanelManager panels = new PanelManager(startMenu, settingMenu, saveListModel, saveStorage);
        PanelStateController.start(panels, navigation);
    }
}

/**
 * a list of configurable constants(updated 11/1):
 * WindowManager.FRAME_WIDTH
 * WindowManager.FRAME_HEIGHT
 * GameController.FRAME_TIME
 */


