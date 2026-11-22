package jswing;

import file.LocalSaveStorage;
import game.GameController;
import game.World;
import interface_adapter.navigation.NavigationViewModel;
import interface_adapter.navigation.ScreenId;
import interface_adapter.save_list.SaveListViewModel;
import java.io.IOException;
import javax.swing.*;
import use_case.save.SaveId;

/**
 * Assembles the Swing panels and maps screen IDs to them.
 * The Load action initializes and starts the selected world.
 */

public class PanelManager {

    private final JPanel startMenuPanel;
    private final JPanel settingMenuPanel;
    private final JPanel loadMenuPanel;
    private final JPanel newWorldPanel;
    private final JPanel gamePanel = createGamePanel();
    private final NavigationViewModel navigation;

    public PanelManager(
            StartMenuPanel startMenuPanel, SettingMenuPanel settingMenuPanel,
            SaveListViewModel saveList, LocalSaveStorage saveStorage, NavigationViewModel navigation) {
        this.startMenuPanel = startMenuPanel;
        this.settingMenuPanel = settingMenuPanel;
        this.navigation = navigation;
        Runnable onBack = () -> navigation.requestScreen(ScreenId.TITLE);
        this.newWorldPanel = createNewWorldPanel(onBack);
        this.loadMenuPanel = new SaveMenuPanel(
                saveList,
                selectionKey -> loadGame(saveStorage, selectionKey),
                onBack);
    }

    public JPanel panelFor(ScreenId screen) {
        return switch (screen) {
            case TITLE -> startMenuPanel;
            case SAVES -> loadMenuPanel;
            case NEW_WORLD -> newWorldPanel;
            case SETTINGS -> settingMenuPanel;
            case GAME -> gamePanel;
        };
    }

    private void loadGame(LocalSaveStorage saveStorage, String selectionKey) {
        try {
            World world = saveStorage.loadSave(new SaveId(selectionKey));
            GameController.initializeGame(world, gamePanel);
            navigation.requestScreen(ScreenId.GAME);
            GameController.start();
        } catch (IOException | RuntimeException exception) {
            navigation.requestScreen(ScreenId.SAVES);
            System.getLogger(PanelManager.class.getName())
                    .log(System.Logger.Level.WARNING, "Could not load the selected world", exception);
            JOptionPane.showMessageDialog(loadMenuPanel, "Could not load the selected world.",
                    "Load failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static JPanel createNewWorldPanel(Runnable onBack) {
        JPanel panel = new JPanel();
        JButton back = new JButton("Back");
        back.addActionListener(event -> onBack.run());
        panel.add(new JLabel("No saved worlds found."));
        panel.add(back);
        return panel;
    }

    private static JPanel createGamePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(null);
        return panel;
    }
}
