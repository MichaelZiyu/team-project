package jswing;

import file.LocalSaveStorage;
import game.GameController;
import game.World;
import interface_adapter.save_list.SaveListViewModel;
import java.io.IOException;
import javax.swing.*;
import states.PanelStateController;
import use_case.save.SaveId;

/**
 * Assembly of swing panels and a bridge to the existing Load action.
 * It supplies panel instances to {@code PanelStateManager}. The Load callback
 * reads the selected world through {@code LocalSaveStorage}, initializes
 * {@code GameController}, and requests the gameplay transition.
 */

public class PanelManager {

    public final JPanel startMenuPanel;
    public final JPanel settingMenuPanel = createSettingMenuPanel();
    public final JPanel loadMenuPanel;
    public final JPanel newWorldPanel = createNewWorldPanel();
    public final JPanel gamePanel = createGamePanel();

    public PanelManager(StartMenuPanel startMenuPanel, SaveListViewModel saveList, LocalSaveStorage saveStorage) {
        this.startMenuPanel = startMenuPanel;
        this.loadMenuPanel = new SaveMenuPanel(
                saveList,
                selectionKey -> loadGame(saveStorage, selectionKey),
                () -> PanelStateController.transition("Exit"));
    }

    private static JPanel createSettingMenuPanel() {
        return new SettingMenuPanel(() -> PanelStateController.transition("Exit"));
    }

    private void loadGame(LocalSaveStorage saveStorage, String selectionKey) {
        try {
            World world = saveStorage.loadSave(new SaveId(selectionKey));
            GameController.initializeGame(world, gamePanel);
        } catch (IOException | RuntimeException exception) {
            System.getLogger(PanelManager.class.getName())
                    .log(System.Logger.Level.WARNING, "Could not load the selected world", exception);
            JOptionPane.showMessageDialog(loadMenuPanel, "Could not load the selected world.",
                    "Load failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        PanelStateController.transition("load");
    }

    private static JPanel createNewWorldPanel() {
        JPanel panel = new JPanel();
        JButton back = new JButton("Back");
        back.addActionListener(event -> PanelStateController.transition("Exit"));
        panel.add(new JLabel("No saved worlds found."));
        panel.add(back);
        return panel;
    }

    private static JPanel createGamePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(null);
        return panel;
    }

    private static JPanel createPausePanel() {
        JPanel panel = new JPanel();

        JButton returnButton = new JButton("return the game");

        returnButton.addActionListener(e -> {
            PanelStateController.transition("return");
        });

        panel.add(returnButton);
        return panel;
    }
}
