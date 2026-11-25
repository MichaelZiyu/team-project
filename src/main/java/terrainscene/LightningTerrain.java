package terrainscene;

import game.gameModel.Renderable;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import jswing.Texture;
import terrain.Terrain;
import terrain.TerrainCoordinates;
import terrain.Trees;

/**
 * The terrain as a lightning strike lights it, shown in place of the usual terrain for the split
 * second of a strike.
 *
 * <p>A strike is too short to repaint the terrain when it happens, so the strike's picture is
 * painted ahead of time: {@link #prepare} paints the area around the player, lit by the strike
 * ({@link TerrainLight#lightningStrike}), on a background thread. {@link #isReadyFor} says whether
 * that picture is done and still covers the screen; the strike should wait until it is. While
 * {@link #setShowing} is on, the layer holds the picture, which covers the usual terrain; put the
 * layer just above the terrain in the render order, under characters. In the strike's glare the
 * water turns as dark as the sides of the blocks, and only its ripples catch the light.
 *
 * <p>Call everything except the painting on the Swing thread.
 */
public final class LightningTerrain implements Iterable<Renderable> {

  /** Paints a rectangle of terrain; lets tests replace the painter. */
  @FunctionalInterface
  interface AreaSource {

    /**
     * Paints a rectangle of tiles.
     *
     * @param firstX the left column, in blocks
     * @param firstY the bottom row, in blocks
     * @param widthBlocks how many columns
     * @param heightBlocks how many rows
     * @param light the light to paint in
     * @return the painted area
     */
    AreaImage paint(int firstX, int firstY, int widthBlocks, int heightBlocks, TerrainLight light);
  }

  /** A painted strike picture, the light it was painted in, and the tiles it covers. */
  private record Prepared(
      TerrainLight light, int minX, int minY, int maxX, int maxY, AreaImage area) {}

  /**
   * Extra tiles painted around the screen on every side, so the player can walk a little between
   * the picture being painted and the strike.
   */
  static final int MARGIN_BLOCKS = 4;

  private final AreaSource source;
  private final Executor executor;
  private final int viewWidthPx;
  private final int viewHeightPx;

  /** The newest picture finished; written by the painting thread. */
  private volatile Prepared ready;

  /** Whether a picture is being painted right now; written by both threads. */
  private volatile boolean painting;

  /** The strike last asked for. */
  private TerrainLight requested;

  /** The ripples over the strike's water; {@code null} in tests, which leave them out. */
  private final RippleSheet ripples;

  /** The picture last shown, and its sprites, reused for its flicker. */
  private Prepared shown;
  private List<Renderable> shownSprites = List.of();
  private List<Renderable> contents = List.of();

  /**
   * Creates strike pictures of the given terrain, painted on their own background thread.
   *
   * @param terrain the terrain to paint
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   */
  public LightningTerrain(Terrain terrain, int viewWidthPx, int viewHeightPx) {
    this(terrain, null, viewWidthPx, viewHeightPx);
  }

  /**
   * Creates strike pictures of the given terrain and the shadows its trees throw in the flash,
   * painted on their own background thread.
   *
   * @param terrain the terrain to paint
   * @param trees the trees growing on it; {@code null} for none
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   */
  public LightningTerrain(Terrain terrain, Trees trees, int viewWidthPx, int viewHeightPx) {
    this(
        new ChunkPainter(terrain, trees, new TerrainImages())::paintArea,
        Executors.newSingleThreadExecutor(
            task -> {
              Thread thread = new Thread(task, "terrain-lightning");
              thread.setDaemon(true);
              return thread;
            }),
        viewWidthPx,
        viewHeightPx,
        new RippleSheet(viewWidthPx, viewHeightPx));
  }

  /**
   * Creates strike pictures with a given way of painting and running work, and no ripples; used
   * by tests.
   *
   * @param source paints an area
   * @param executor runs painting work, normally on a background thread
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   */
  LightningTerrain(AreaSource source, Executor executor, int viewWidthPx, int viewHeightPx) {
    this(source, executor, viewWidthPx, viewHeightPx, null);
  }

  private LightningTerrain(
      AreaSource source,
      Executor executor,
      int viewWidthPx,
      int viewHeightPx,
      RippleSheet ripples) {
    this.source = source;
    this.executor = executor;
    this.viewWidthPx = viewWidthPx;
    this.viewHeightPx = viewHeightPx;
    this.ripples = ripples;
  }

  /**
   * Starts painting the area around the player lit by a strike, unless a picture for that strike
   * is already painted, or being painted, and covers the screen.
   *
   * @param strike the strike's light
   * @param playerX the player's x position, in pixels; the camera is centred on it
   * @param playerY the player's y position, in pixels
   */
  public void prepare(TerrainLight strike, double playerX, double playerY) {
    if (strike.equals(requested) && (painting || isReadyFor(strike, playerX, playerY))) {
      return;
    }
    requested = strike;
    painting = true;
    int[] view = viewBlocks(playerX, playerY);
    int minX = view[0] - MARGIN_BLOCKS;
    int minY = view[1] - MARGIN_BLOCKS;
    int maxX = view[2] + MARGIN_BLOCKS;
    int maxY = view[3] + MARGIN_BLOCKS;
    executor.execute(
        () -> {
          try {
            AreaImage area = source.paint(minX, minY, maxX - minX + 1, maxY - minY + 1, strike);
            ready = new Prepared(strike, minX, minY, maxX, maxY, area);
          } catch (RuntimeException e) {
            System.err.println("Could not paint the lightning terrain: " + e);
          } finally {
            painting = false;
          }
        });
  }

  /**
   * Returns whether a picture for this strike is painted and covers the screen at the player's
   * position.
   *
   * @param strike the strike's light
   * @param playerX the player's x position, in pixels
   * @param playerY the player's y position, in pixels
   * @return {@code true} if the strike can be shown now
   */
  public boolean isReadyFor(TerrainLight strike, double playerX, double playerY) {
    Prepared picture = ready;
    if (picture == null || !picture.light().equals(strike)) {
      return false;
    }
    int[] view = viewBlocks(playerX, playerY);
    return view[0] >= picture.minX()
        && view[1] >= picture.minY()
        && view[2] <= picture.maxX()
        && view[3] <= picture.maxY();
  }

  /**
   * Shows or hides the strike picture. It is only shown if one is painted.
   *
   * @param showing {@code true} for the instant of the strike
   */
  public void setShowing(boolean showing) {
    Prepared picture = ready;
    if (!showing || picture == null) {
      contents = List.of();
      return;
    }
    if (picture != shown) {
      shown = picture;
      shownSprites = sprites(picture);
    }
    contents = shownSprites;
  }

  /**
   * Returns a picture's sprites, top first: the lit terrain, then (if it has water) the ripples,
   * glaring in the strike's light, and the darkened water under them, so that only the ripples
   * catch the light, and the strike's shadows still fall across them.
   */
  private List<Renderable> sprites(Prepared picture) {
    AreaImage area = picture.area();
    Renderable terrainSprite = new AreaRenderable(area.image(), area.leftPx(), area.bottomPx());
    if (area.water() == null) {
      return List.of(terrainSprite);
    }
    Renderable waterSprite = new AreaRenderable(area.water(), area.leftPx(), area.bottomPx());
    if (ripples == null) {
      return List.of(terrainSprite, waterSprite);
    }
    ripples.cover(
        area.leftPx(), area.bottomPx(), area.water().getWidth(), area.water().getHeight());
    double strength = picture.light().topGlow() / TerrainLight.STRIKE_TOP_GLOW;
    ripples.setBrightness(1 + (RippleSheet.MAX_BRIGHTNESS - 1) * Math.min(1, strength));
    return List.of(terrainSprite, ripples, waterSprite);
  }

  /**
   * Returns whether the strike picture is showing.
   *
   * @return {@code true} while it covers the usual terrain
   */
  public boolean isShowing() {
    return !contents.isEmpty();
  }

  @Override
  public Iterator<Renderable> iterator() {
    return contents.iterator();
  }

  /**
   * Returns the tiles the screen shows at a player position, as {@code {minX, minY, maxX, maxY}}
   * in blocks, including the rows below the screen whose tall tiles are lifted into view.
   */
  private int[] viewBlocks(double playerX, double playerY) {
    return new int[] {
      TerrainCoordinates.pixelToBlock(playerX - viewWidthPx / 2.0),
      TerrainCoordinates.pixelToBlock(
          playerY - viewHeightPx / 2.0 - ChunkedTerrainScene.MAX_LIFT_PX),
      TerrainCoordinates.pixelToBlock(playerX + viewWidthPx / 2.0),
      TerrainCoordinates.pixelToBlock(playerY + viewHeightPx / 2.0)
    };
  }

  /** A painted picture placed in the world as a sprite. */
  private static final class AreaRenderable implements Renderable {

    private static final long serialVersionUID = 1L;

    private final transient BufferedImage image;
    private final int leftPx;
    private final int bottomPx;

    /** Made on first use, on the Swing thread, and never saved. */
    private transient Texture texture;

    AreaRenderable(BufferedImage image, int leftPx, int bottomPx) {
      this.image = image;
      this.leftPx = leftPx;
      this.bottomPx = bottomPx;
    }

    @Override
    public Texture getTexture() {
      if (texture == null) {
        texture = new ImageTexture(image);
      }
      return texture;
    }

    /** Returns the picture's centre, as the renderer places sprites by their centre. */
    @Override
    public int getX() {
      return leftPx + image.getWidth() / 2;
    }

    /** Returns the picture's bottom edge. */
    @Override
    public int getY() {
      return bottomPx;
    }

    @Override
    public int getWidth() {
      return image.getWidth();
    }

    @Override
    public int getHeight() {
      return image.getHeight();
    }
  }
}
