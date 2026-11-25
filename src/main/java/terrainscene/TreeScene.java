package terrainscene;

import game.gameModel.Renderable;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jswing.Texture;
import terrain.TerrainCoordinates;
import terrain.Tree;
import terrain.Trees;

/**
 * The trees around the player, split into those behind the player and those in front.
 *
 * <p>A tree is behind the player while the player stands nearer the bottom of the screen than the
 * middle of its trunk, and in front once the player walks further up the screen than that. A tree
 * in front that covers the player fades to {@link #SEE_THROUGH_OPACITY}, so the player can still
 * be seen behind it, and back to solid once it no longer covers them; both fades take a moment.
 *
 * <p>Call {@link #update} once a game tick. The two lists it makes are drawn by the renderer
 * from its own thread: {@link #behind} under the characters and {@link #inFront} over them. Each
 * list puts nearer trees first, since the renderer draws the first sprite on top.
 */
public final class TreeScene {

  /** How solid a tree in front of the player is while it covers them. */
  static final double SEE_THROUGH_OPACITY = 0.55;

  /** How fast a tree fades out or in, as a share of the change per second. */
  static final double FADE_PER_SECOND = 3;

  /** How far past the screen's edges to look for trees, in pixels: a tall tree's full height. */
  static final int MARGIN_PX = 512;

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  private final Trees trees;
  private final TreePainter painter;
  private final int viewWidthPx;
  private final int viewHeightPx;
  private final Map<Tree, TreeRenderable> shown = new HashMap<>();
  private volatile List<Renderable> behind = List.of();
  private volatile List<Renderable> inFront = List.of();

  /**
   * Creates the scene.
   *
   * @param trees where trees grow
   * @param painter paints them
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   */
  public TreeScene(Trees trees, TreePainter painter, int viewWidthPx, int viewHeightPx) {
    this.trees = trees;
    this.painter = painter;
    this.viewWidthPx = viewWidthPx;
    this.viewHeightPx = viewHeightPx;
  }

  /**
   * Brings the trees up to date around the player.
   *
   * @param playerX the middle of the player's sprite, in world pixels; the camera follows it
   * @param playerY the bottom of the player's sprite (their feet), in world pixels
   * @param playerWidth the player's sprite's width, in pixels
   * @param playerHeight the player's sprite's height, in pixels
   * @param seconds how much time passed since the last update, in seconds
   */
  public void update(
      double playerX, double playerY, int playerWidth, int playerHeight, double seconds) {
    int minX = TerrainCoordinates.pixelToBlock(playerX - viewWidthPx / 2.0 - MARGIN_PX);
    int maxX = TerrainCoordinates.pixelToBlock(playerX + viewWidthPx / 2.0 + MARGIN_PX);
    int minY = TerrainCoordinates.pixelToBlock(playerY - viewHeightPx / 2.0 - MARGIN_PX);
    int maxY = TerrainCoordinates.pixelToBlock(playerY + viewHeightPx / 2.0 + MARGIN_PX);
    List<Tree> near = trees.within(minX, minY, maxX, maxY);
    shown.keySet().retainAll(near);
    List<TreeRenderable> back = new ArrayList<>();
    List<TreeRenderable> front = new ArrayList<>();
    for (Tree tree : near) {
      TreeRenderable sprite = shown.computeIfAbsent(tree, TreeRenderable::new);
      boolean playerBehindIt = playerY >= sprite.depthY();
      boolean covers =
          playerBehindIt && sprite.covers(playerX, playerY, playerWidth, playerHeight);
      sprite.fadeTowards(covers ? SEE_THROUGH_OPACITY : 1, seconds);
      (playerBehindIt ? front : back).add(sprite);
    }
    Comparator<TreeRenderable> nearestFirst = Comparator.comparingInt(TreeRenderable::depthY);
    back.sort(nearestFirst);
    front.sort(nearestFirst);
    behind = List.copyOf(back);
    inFront = List.copyOf(front);
  }

  /**
   * Returns the trees behind the player, to draw under the characters.
   *
   * @return the trees, nearest first
   */
  public Iterable<Renderable> behind() {
    return () -> behind.iterator();
  }

  /**
   * Returns the trees in front of the player, to draw over the characters.
   *
   * @return the trees, nearest first
   */
  public Iterable<Renderable> inFront() {
    return () -> inFront.iterator();
  }

  /** One tree's picture, placed by the game camera's rule: by its middle and its bottom edge. */
  final class TreeRenderable implements Renderable {

    private static final long serialVersionUID = 1L;

    private final Tree tree;
    private final int[] bounds;
    private double opacity = 1;
    private double shownOpacity = Double.NaN;

    /** The picture at the current opacity; replaced on the game's thread as the tree fades. */
    private transient volatile BufferedImage image;

    /** The texture showing {@link #textureImage}; made on first use, on the Swing thread. */
    private transient Texture texture;

    private transient BufferedImage textureImage;

    TreeRenderable(Tree tree) {
      this.tree = tree;
      this.bounds = TreePainter.bounds(tree.shape());
    }

    /** Returns the y the player is compared with: the middle of the trunk's tile, as drawn. */
    int depthY() {
      return TerrainCoordinates.blockToPixel(tree.blockY())
          + tree.groundHeight() * TileRenderable.STEP_HEIGHT_PX
          + BLOCK / 2;
    }

    /** Returns whether the tree's picture overlaps the player's sprite. */
    boolean covers(double playerX, double playerY, int playerWidth, int playerHeight) {
      double left = getX() - getWidth() / 2.0;
      double bottom = getY();
      return playerX + playerWidth / 2.0 > left
          && playerX - playerWidth / 2.0 < left + getWidth()
          && playerY + playerHeight > bottom
          && playerY < bottom + getHeight();
    }

    /** Moves the opacity a step towards a target, repainting only when it visibly changes. */
    void fadeTowards(double target, double seconds) {
      double step = FADE_PER_SECOND * seconds;
      opacity += Math.max(-step, Math.min(step, target - opacity));
      double rounded = Math.round(opacity * 20) / 20.0;
      if (rounded != shownOpacity) {
        shownOpacity = rounded;
        image = painter.paint(tree.shape(), rounded);
      }
    }

    /** Returns how solid the tree is right now, from 0 to 1. */
    double opacity() {
      return opacity;
    }

    @Override
    public Texture getTexture() {
      BufferedImage current = image;
      if (current != textureImage) {
        // A texture of its own: the renderer shows each texture as one component, once.
        textureImage = current;
        texture = current == null ? null : new ImageTexture(current);
      }
      return texture;
    }

    @Override
    public int getX() {
      return TerrainCoordinates.blockToPixel(tree.blockX()) + BLOCK / 2;
    }

    @Override
    public int getY() {
      return TerrainCoordinates.blockToPixel(tree.blockY())
          + tree.groundHeight() * TileRenderable.STEP_HEIGHT_PX
          + bounds[0];
    }

    @Override
    public int getWidth() {
      return TreePainter.width(tree.shape());
    }

    @Override
    public int getHeight() {
      return bounds[1];
    }
  }
}
