package jswing;

import states.PanelState;

import javax.swing.*;

/**
 * WindowManager is a class that stores the game frame and show any provided state(except exitState) from the game
 */
public class WindowManager {

    /** the main frame of the game*/
    private JFrame frame;

    /**default frame width and height*/
    private static final int FRAME_WIDTH = 800;
    private static final int FRAME_HEIGHT = 600;

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
