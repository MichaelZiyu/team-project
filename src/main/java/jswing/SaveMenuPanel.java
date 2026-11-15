package jswing;

import interface_adapter.save_list.SaveListViewModel;
import interface_adapter.save_list.SaveListViewModel.Option;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

/**
 * Swing view for selecting a saved world.
 * It observes {@code SaveListViewModel} to refresh the picker and forwards
 * the selected key through its injected Load callback. A second callback
 * handles leaving the picker. UI for the browse use case.
 */

public final class SaveMenuPanel extends JPanel {
    private final JComboBox<Option> saveList = new JComboBox<>();
    private final JButton loadButton = new JButton("Test Text for load");

    public SaveMenuPanel(SaveListViewModel viewModel, Consumer<String> onLoad, Runnable onBack) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        saveList.setMaximumSize(new Dimension(200, 30));
        saveList.setPreferredSize(new Dimension(200, 30));
        saveList.setAlignmentX(Component.CENTER_ALIGNMENT);

        loadButton.addActionListener(event -> {
            if (saveList.getSelectedItem() instanceof Option selected) {
                onLoad.accept(selected.selectionKey());
            }
        });

        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(event -> onBack.run());

        add(saveList);
        add(loadButton);
        add(exitButton);
        viewModel.addOptionsListener(this::showSaves);
    }

    private void showSaves(List<Option> options) {
        Object previouslySelected = saveList.getSelectedItem();
        saveList.setModel(new DefaultComboBoxModel<>(options.toArray(Option[]::new)));
        if (previouslySelected != null && options.contains(previouslySelected)) {
            saveList.setSelectedItem(previouslySelected);
        }
        loadButton.setEnabled(!options.isEmpty());
    }
}
