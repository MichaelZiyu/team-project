package jswing;

import java.awt.Dimension;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;

/** Owns the Swing frame and displays the panel selected by the application. */
public class WindowManager {

    private final JFrame frame;

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

    public void setWindow(JPanel panel) {
        frame.setContentPane(panel);
        frame.revalidate();
        frame.repaint();
    }

    public void open() {
        frame.setVisible(true);
    }

    public void requestClose() {
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
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

        return frame;
    }
}
