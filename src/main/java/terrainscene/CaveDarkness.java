package terrainscene;

import game.gameModel.Renderable;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.List;
import jswing.Texture;
import terrain.CaveTerrain;
import terrain.TerrainCoordinates;

/**
 * The darkness of a cave, as a layer over everything: a dim olive-brown gloom, with a soft circle
 * of light around the player, as if they carried a lantern.
 *
 * <p>Near the player the cave is only dimmed, with a warm tint; from {@link #LIT_RADIUS_PX} it
 * fades smoothly to the gloom at {@link #DARK_RADIUS_PX}. The gloom is not quite opaque, so the
 * cave far from the player stays just visible, tinted like the rock in lamplight. The tops of the
 * solid rock are darkened further ({@link #ROCK_TOP}), lantern or not: only the floor and the
 * faces of the rock walls catch the light, so the walls read as cliffs around the open floor.
 *
 * <p>The camera always centres on the player, so the light is painted once; each frame only the
 * rock tops are added. The layer reaches {@link #BORDER_PX} past the screen's edges, so its edge
 * never shows when the player moves between frames. Put it in front of the terrain and characters
 * in the render order, as weather is.
 */
public final class CaveDarkness implements Iterable<Renderable> {

  /** How far from the player the lantern is at its brightest, in pixels. */
  public static final double LIT_RADIUS_PX = 70;

  /** How far from the player the light has faded fully to gloom, in pixels. */
  public static final double DARK_RADIUS_PX = 430;

  /** How much of the cave the gloom hides even right by the player, from 0 to 1. */
  static final double LIT_OPACITY = 0.22;

  /** The colour over the tops of solid rock, which the lantern does not reach. */
  static final Color ROCK_TOP = new Color(8, 7, 3, 215);

  /** The colour of the gloom far from the player. */
  static final Color GLOOM = new Color(18, 16, 7);

  /** How much of the cave the gloom hides, from 0 (none) to 1 (all). */
  static final double GLOOM_OPACITY = 0.9;

  /** The warm colour of the dimming right around the player. */
  static final Color LANTERN = new Color(70, 52, 22);

  /** How far the light's middle is above the player's feet, in pixels, so it centres on them. */
  static final int LIGHT_ABOVE_FEET_PX = 32;

  /** How far the layer reaches past each screen edge, in pixels. */
  static final int BORDER_PX = 96;

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  private final CaveTerrain cave;
  private final int width;
  private final int height;
  private final int[] light;
  private final BufferedImage image;
  private final Overlay overlay = new Overlay();
  private final List<Renderable> contents = List.of(overlay);
  private int centreX;
  private int bottomY;

  /**
   * Creates the darkness for a screen of the given size.
   *
   * @param cave the cave seen from above, to tell its rock from its floor
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   */
  public CaveDarkness(CaveTerrain cave, int viewWidthPx, int viewHeightPx) {
    this(viewWidthPx, viewHeightPx, cave);
  }

  /**
   * Creates the darkness of a cave seen from the side: the gloom and the lantern's light, with no
   * rock tops to darken.
   *
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   */
  public CaveDarkness(int viewWidthPx, int viewHeightPx) {
    this(viewWidthPx, viewHeightPx, null);
  }

  private CaveDarkness(int viewWidthPx, int viewHeightPx, CaveTerrain cave) {
    this.cave = cave;
    this.width = viewWidthPx + 2 * BORDER_PX;
    this.height = viewHeightPx + 2 * BORDER_PX;
    this.light = paintLight(width, height);
    this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    image.setRGB(0, 0, width, height, light, 0, width);
  }

  /**
   * Centres the light on the player.
   *
   * @param playerX the player's x position, in pixels; the camera is centred on it
   * @param playerY the player's y position, in pixels
   */
  public void update(double playerX, double playerY) {
    // The renderer places sprites relative to the player's whole-pixel position.
    int cameraX = (int) playerX;
    int cameraY = (int) playerY;
    centreX = cameraX;
    bottomY = cameraY - height / 2;
    image.setRGB(0, 0, width, height, light, 0, width);
    if (cave == null) {
      return;
    }
    Graphics2D g = image.createGraphics();
    try {
      g.setColor(ROCK_TOP);
      int lift = CaveTerrain.WALL_HEIGHT * TileRenderable.STEP_HEIGHT_PX;
      // Every tile whose lifted top could be on the layer: rows below the screen are lifted in.
      int firstX = Math.floorDiv(cameraX - width / 2, BLOCK) - 1;
      int lastX = Math.floorDiv(cameraX + width / 2, BLOCK) + 1;
      int firstY = Math.floorDiv(cameraY - height / 2 - lift, BLOCK) - 1;
      int lastY = Math.floorDiv(cameraY + height / 2, BLOCK) + 1;
      for (int blockX = firstX; blockX <= lastX; blockX++) {
        for (int blockY = firstY; blockY <= lastY; blockY++) {
          if (!cave.isOpen(blockX, blockY)) {
            // The game camera's rule, for the tile's top lifted by the rock's height.
            int x = blockX * BLOCK - cameraX + width / 2;
            int y = height / 2 - ((blockY + 1) * BLOCK + lift - cameraY);
            g.fillRect(x, y, BLOCK, BLOCK);
          }
        }
      }
    } finally {
      g.dispose();
    }
  }

  /**
   * Returns how dark the layer is at a distance from the light's middle.
   *
   * @param distancePx the distance, in pixels
   * @return from 0 (fully lit) to 1 (full gloom)
   */
  static double darknessAt(double distancePx) {
    double t = (distancePx - LIT_RADIUS_PX) / (DARK_RADIUS_PX - LIT_RADIUS_PX);
    t = Math.max(0, Math.min(1, t));
    return t * t * (3 - 2 * t);
  }

  /** Returns the painted layer, for tests. */
  BufferedImage image() {
    return image;
  }

  @Override
  public Iterator<Renderable> iterator() {
    return contents.iterator();
  }

  /** Paints the lantern's light fading into gloom, as ARGB pixels row by row. */
  private static int[] paintLight(int width, int height) {
    int[] pixels = new int[width * height];
    double lightX = width / 2.0;
    double lightY = height / 2.0 - LIGHT_ABOVE_FEET_PX;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        double dark = darknessAt(Math.hypot(x + 0.5 - lightX, y + 0.5 - lightY));
        // From the lantern's warm dimness near the player to the gloom far away.
        double opacity = LIT_OPACITY + (GLOOM_OPACITY - LIT_OPACITY) * dark;
        int red = mix(LANTERN.getRed(), GLOOM.getRed(), dark);
        int green = mix(LANTERN.getGreen(), GLOOM.getGreen(), dark);
        int blue = mix(LANTERN.getBlue(), GLOOM.getBlue(), dark);
        int alpha = (int) Math.round(opacity * 255);
        pixels[y * width + x] = alpha << 24 | red << 16 | green << 8 | blue;
      }
    }
    return pixels;
  }

  private static int mix(int from, int to, double amount) {
    return (int) Math.round(from + (to - from) * amount);
  }

  /** The screen-sized sprite showing the darkness. */
  private final class Overlay implements Renderable {

    private static final long serialVersionUID = 1L;

    /** Made on first use, on the Swing thread, and never saved. */
    private transient Texture texture;

    @Override
    public Texture getTexture() {
      if (texture == null) {
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
