package terrainscene;

import game.gameModel.Renderable;
import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
import javax.swing.JLabel;
import jswing.Texture;

/**
 * The moving ripples over the water: a sheet tiled with the current frame of {@link
 * WaterRipples}.
 *
 * <p>It is drawn just above the water's colour and below the terrain, whose solid tiles hide it
 * everywhere but the water, and whose shadows still fall across it. The tiles are fixed to the
 * world, so the ripples stay put as the camera moves. The animation is timed by the clock, so it
 * runs at the same speed however often the game draws. The sheet either follows the camera
 * ({@link #moveTo}) or covers a fixed area ({@link #cover}), and can be drawn brighter, as in the
 * glare of lightning ({@link #setBrightness}).
 */
final class RippleSheet implements Renderable {

  private static final long serialVersionUID = 1L;

  /** How far the sheet reaches past each edge of the screen when following the camera, in px. */
  static final int MARGIN_PX = 64;

  /** The brightest the ripples can be drawn, as a number of layers of the usual highlights. */
  static final double MAX_BRIGHTNESS = 4;

  private static final int TILE = WaterRipples.TILE_PX;

  private static final long NANOS_PER_SECOND = 1_000_000_000L;

  private final int viewWidthPx;
  private final int viewHeightPx;
  private final long startNanos = System.nanoTime();

  /** Worked out in the background, and never saved. */
  private final transient CompletableFuture<WaterRipples> ripples;

  /** Holds a Swing component, so never saved. */
  private final transient Texture texture;

  private volatile int leftPx;
  private volatile int bottomPx;
  private volatile int widthPx;
  private volatile int heightPx;
  private volatile double brightness = 1;

  /**
   * Creates a sheet showing the game's shared ripples, sized to follow the camera.
   *
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   */
  RippleSheet(int viewWidthPx, int viewHeightPx) {
    this(viewWidthPx, viewHeightPx, WaterRipples.shared());
  }

  /**
   * Creates a sheet showing some ripples, sized to follow the camera.
   *
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   * @param ripples the ripples, once they are ready
   */
  RippleSheet(int viewWidthPx, int viewHeightPx, CompletableFuture<WaterRipples> ripples) {
    this.viewWidthPx = viewWidthPx;
    this.viewHeightPx = viewHeightPx;
    this.ripples = ripples;
    this.texture = new SheetTexture();
    moveTo(0, 0);
  }

  /**
   * Covers the screen around the camera, with a margin.
   *
   * @param cameraX the camera's x position, in pixels
   * @param cameraY the camera's y position, in pixels
   */
  void moveTo(double cameraX, double cameraY) {
    int width = viewWidthPx + 2 * MARGIN_PX;
    int height = viewHeightPx + 2 * MARGIN_PX;
    cover(
        (int) Math.round(cameraX) - width / 2,
        (int) Math.round(cameraY) - height / 2,
        width,
        height);
  }

  /**
   * Covers a fixed area of the world.
   *
   * @param leftPx the world x of its left edge, in pixels
   * @param bottomPx the world y of its bottom edge, in pixels
   * @param widthPx its width, in pixels
   * @param heightPx its height, in pixels
   */
  void cover(int leftPx, int bottomPx, int widthPx, int heightPx) {
    this.leftPx = leftPx;
    this.bottomPx = bottomPx;
    this.widthPx = widthPx;
    this.heightPx = heightPx;
  }

  /**
   * Sets how bright the ripples are drawn.
   *
   * @param brightness 1 as usual, up to {@link #MAX_BRIGHTNESS} for the brightest; each extra 1
   *     lays the highlights on once more
   * @throws IllegalArgumentException if {@code brightness} is out of range
   */
  void setBrightness(double brightness) {
    if (!(brightness >= 1 && brightness <= MAX_BRIGHTNESS)) {
      throw new IllegalArgumentException(
          "brightness must be in [1, " + MAX_BRIGHTNESS + "]: " + brightness);
    }
    this.brightness = brightness;
  }

  /**
   * Returns whether the animation is ready to show.
   *
   * @return {@code false} while it is still being worked out
   */
  boolean isReady() {
    return ripples.isDone() && !ripples.isCompletedExceptionally();
  }

  @Override
  public Texture getTexture() {
    return texture;
  }

  /** Returns the sheet's centre, as the renderer places sprites by their centre. */
  @Override
  public int getX() {
    return leftPx + widthPx / 2;
  }

  /** Returns the sheet's bottom edge. */
  @Override
  public int getY() {
    return bottomPx;
  }

  @Override
  public int getWidth() {
    return widthPx;
  }

  @Override
  public int getHeight() {
    return heightPx;
  }

  /**
   * Draws the current frame tiled across the sheet, lined up with the world, laid on as many
   * times as the brightness says.
   *
   * @param g where to draw; {@code (0, 0)} is the sheet's top-left corner
   */
  void paintSheet(Graphics g) {
    if (!isReady()) {
      return;
    }
    double seconds = (System.nanoTime() - startNanos) / (double) NANOS_PER_SECOND;
    BufferedImage frame = ripples.join().frameAt(seconds);
    Graphics2D g2 = (Graphics2D) g.create();
    try {
      double layers = brightness;
      while (layers > 0) {
        float opacity = (float) Math.min(1, layers);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        tile(g2, frame);
        layers -= 1;
      }
    } finally {
      g2.dispose();
    }
  }

  /** Tiles a frame across the sheet, lined up with the world's grid of tiles. */
  private void tile(Graphics2D g, BufferedImage frame) {
    int width = widthPx;
    int height = heightPx;
    int top = bottomPx + height;
    // World y grows upward, the sheet's downward.
    int firstX = -Math.floorMod(leftPx, TILE);
    int firstY = -Math.floorMod(-top, TILE);
    for (int x = firstX; x < width; x += TILE) {
      for (int y = firstY; y < height; y += TILE) {
        g.drawImage(frame, x, y, null);
      }
    }
  }

  /** A texture that draws the sheet's current frame whenever Swing paints it. */
  private final class SheetTexture extends Texture {

    private final JLabel label =
        new JLabel() {
          @Override
          protected void paintComponent(Graphics g) {
            // Named in full: inside a label, plain paint() would be the label's own, which
            // paints the label again, forever.
            RippleSheet.this.paintSheet(g);
          }
        };

    SheetTexture() {
      // The parent loads images from a folder; any existing one will do, since none is used.
      super(TileRenderable.TEXTURE_FOLDER + "air");
      label.setOpaque(false);
    }

    @Override
    public void tick() {}

    @Override
    public JLabel getContent() {
      return label;
    }

    @Override
    public int getWidth() {
      return widthPx;
    }

    @Override
    public int getHeight() {
      return heightPx;
    }
  }
}
