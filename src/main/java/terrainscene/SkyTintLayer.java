package terrainscene;

import game.gameModel.Renderable;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import jswing.Texture;

/**
 * The time-of-day wash over the whole screen ({@link SkyTint}), as a layer for the renderer.
 *
 * <p>It must be drawn on top of the world, terrain and characters alike, so it goes first in the
 * render order (the renderer draws the first sprite on top). Call {@link #update} on the Swing
 * thread before each frame. In the middle of the day there is no wash, and the layer is empty.
 */
public final class SkyTintLayer implements Iterable<Renderable> {

  /**
   * Extra pixels the wash reaches past each screen edge, so rounding the player's position never
   * leaves a sliver of screen uncovered.
   */
  static final int OVERHANG_PX = 2;

  /**
   * How much any part of the colour (red, green, blue or opacity) must change before the wash is
   * redrawn; it changes slowly, so most frames reuse the same image.
   */
  static final int REDRAW_STEP = 2;

  private final int width;
  private final int height;

  private Overlay overlay;
  private List<Renderable> contents = List.of();

  /**
   * Creates a layer for a screen of the given size.
   *
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   */
  public SkyTintLayer(int viewWidthPx, int viewHeightPx) {
    this.width = viewWidthPx + 2 * OVERHANG_PX;
    this.height = viewHeightPx + 2 * OVERHANG_PX;
  }

  /**
   * Updates the wash for the time of day and keeps it centred on the screen.
   *
   * @param playerX the player's x position, in pixels; the camera is centred on it
   * @param playerY the player's y position, in pixels
   * @param timeOfDay a fraction of a day: 0 is midnight, 0.5 is noon
   */
  public void update(double playerX, double playerY, double timeOfDay) {
    Color colour = SkyTint.at(timeOfDay);
    if (colour.getAlpha() == 0) {
      overlay = null;
      contents = List.of();
      return;
    }
    if (overlay == null || farApart(colour, overlay.colour)) {
      overlay = new Overlay(colour, width, height);
      contents = Collections.singletonList(overlay);
    }
    // The renderer places sprites by centre and bottom edge, relative to the player's whole
    // pixel position; this puts the wash's top-left corner just off the screen's top-left.
    overlay.centreX = (int) playerX;
    overlay.bottomY = (int) playerY - height / 2;
  }

  /**
   * Returns the wash currently shown.
   *
   * @return its colour, or {@code null} when there is none
   */
  Color currentColour() {
    return overlay == null ? null : overlay.colour;
  }

  @Override
  public Iterator<Renderable> iterator() {
    return contents.iterator();
  }

  private static boolean farApart(Color a, Color b) {
    return Math.abs(a.getRed() - b.getRed()) >= REDRAW_STEP
        || Math.abs(a.getGreen() - b.getGreen()) >= REDRAW_STEP
        || Math.abs(a.getBlue() - b.getBlue()) >= REDRAW_STEP
        || Math.abs(a.getAlpha() - b.getAlpha()) >= REDRAW_STEP;
  }

  /** One see-through rectangle covering the screen. */
  private static final class Overlay implements Renderable {

    private static final long serialVersionUID = 1L;

    private final Color colour;
    private final int width;
    private final int height;
    private int centreX;
    private int bottomY;

    /** Made on first use, on the Swing thread, and never saved. */
    private transient Texture texture;

    Overlay(Color colour, int width, int height) {
      this.colour = colour;
      this.width = width;
      this.height = height;
    }

    @Override
    public Texture getTexture() {
      if (texture == null) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
          g.setColor(colour);
          g.fillRect(0, 0, width, height);
        } finally {
          g.dispose();
        }
        texture = new ImageTexture(image);
      }
      return texture;
    }

    @Override
    public int getX() {
      return centreX;
    }

    @Override
    public int getY() {
      return bottomY;
    }

    @Override
    public int getWidth() {
      return width;
    }

    @Override
    public int getHeight() {
      return height;
    }
  }
}
