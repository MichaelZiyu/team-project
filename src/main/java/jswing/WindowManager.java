package jswing;

import states.PanelState;

import javax.swing.*;
import java.awt.Dimension;

/**
 * WindowManager is a class that stores the game frame and show any provided state(except exitState) from the game
 */
public class WindowManager {

    /** the main frame of the game*/
    private JFrame frame;

    public static final int MIN_FRAME_WIDTH = 800;
    public static final int MAX_FRAME_WIDTH = 1600;
    public static final int MIN_FRAME_HEIGHT = 600;
    public static final int MAX_FRAME_HEIGHT = 900;

    /**default frame width and height*/
    public static int FRAME_WIDTH = MIN_FRAME_WIDTH;
    public static int FRAME_HEIGHT = MIN_FRAME_HEIGHT;

    public WindowManager() {
        this.frame = initializeFrame();
    }

    /**
     * show any title state provided(except exitState) by setting the contentpane of current frame to the state's panel,
     * then refresh the frame
     *
     * @param state the title state to show
     *
     */
    public void setWindow(PanelState state) {
        frame.setContentPane(state.panel);
        frame.revalidate();
        frame.repaint();
    }

    /**
     * resize the frame to the given dimensions
     *
     * @param width the width of the frame to resize
     *
     * @param height the height of the frame to resize
     */
    public void setWindowSize(int width, int height) {
        frame.getRootPane().setPreferredSize(new Dimension(width, height));
        frame.pack();
        frame.setLocationRelativeTo(null);
        FRAME_WIDTH = width;
        FRAME_HEIGHT = height;
    }

    /**
     * initialize an exit-on-close JFrame by default sizes
     *
     * @return the JFrame initialized
     *
     */
    private JFrame initializeFrame() {
        JFrame frame = new JFrame("My Game");

        frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        frame.setVisible(true);

        return frame;
    }
}
