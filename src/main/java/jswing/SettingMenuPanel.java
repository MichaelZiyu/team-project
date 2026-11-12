package jswing;

import states.PanelStateController;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

/** Displays the controls for the settings menu. */
public class SettingMenuPanel extends BackgroundMenuPanel {

    // Put image at: src/main/resources/textures/menu/background.png
    private static final String BACKGROUND_PATH =
            "/textures/menu/setting-background.png";

    private static final Color TEXT_COLOR = new Color(240, 235, 220);

    private static final WindowSize STANDARD_SIZE =
            new WindowSize(WindowManager.MIN_FRAME_WIDTH, WindowManager.MIN_FRAME_HEIGHT);
    private static final WindowSize WIDE_SIZE =
            new WindowSize(WindowManager.MAX_FRAME_WIDTH, WindowManager.MAX_FRAME_HEIGHT);

    private final JComboBox<WindowSize> windowSizeInput =
            new JComboBox<>(new WindowSize[]{STANDARD_SIZE, WIDE_SIZE});
    private final JLabel status = new JLabel(" ");

    public SettingMenuPanel(Runnable onBack) {
        super(BACKGROUND_PATH, new GridBagLayout());
        setBackground(new Color(24, 29, 40));

        JPanel card = new JPanel(new BorderLayout(0, 20));
        card.setBackground(new Color(34, 41, 55));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(151, 162, 176), 2),
                BorderFactory.createEmptyBorder(24, 32, 24, 32)));

        JLabel title = new JLabel("Settings", SwingConstants.CENTER);
        title.setFont(new Font(Font.SERIF, Font.BOLD, 38));
        title.setForeground(TEXT_COLOR);
        card.add(title, BorderLayout.NORTH);

        /** Options to change the window size */
        JPanel options = new JPanel(new GridLayout(0, 2, 16, 10));
        options.setOpaque(false);
        addOption(options, "Window size", windowSizeInput);

        /** Options to change the render distance (not implemented) */
        JComboBox<String> renderDistance = new JComboBox<>(new String[]{"Near", "Normal", "Far"});
        renderDistance.setSelectedItem("Normal");
        addOption(options, "Render distance", renderDistance);

        /** Toggle to fullscreen the window (not implemented) */
        JCheckBox fullscreen = new JCheckBox();
        fullscreen.setOpaque(false);
        addOption(options, "Fullscreen", fullscreen);
        card.add(options, BorderLayout.CENTER);

        JButton back = new JButton("Back");
        back.addActionListener(event -> onBack.run());
        JButton apply = new JButton("Apply");
        apply.addActionListener(event -> {
            WindowSize size = (WindowSize) windowSizeInput.getSelectedItem();
            PanelStateController.setWindowSize(size.width(), size.height());
            status.setText("Window size applied.");
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        buttons.setOpaque(false);
        buttons.add(back);
        buttons.add(apply);

        status.setForeground(TEXT_COLOR);
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        footer.add(status, BorderLayout.NORTH);
        footer.add(buttons, BorderLayout.SOUTH);
        card.add(footer, BorderLayout.SOUTH);

        add(card);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        WindowSize currentSize = new WindowSize(WindowManager.FRAME_WIDTH, WindowManager.FRAME_HEIGHT);
        windowSizeInput.setSelectedItem(WIDE_SIZE.equals(currentSize) ? WIDE_SIZE : STANDARD_SIZE);
        status.setText(" ");
    }

    private record WindowSize(int width, int height) {
        @Override
        public String toString() {
            return width + " × " + height;
        }
    }

    private static void addOption(JPanel options, String name, JComponent control) {
        JLabel label = new JLabel(name);
        label.setForeground(TEXT_COLOR);
        label.setLabelFor(control);
        options.add(label);
        options.add(control);
    }
}
