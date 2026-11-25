package terrainscene;

import cave.SideCave;
import game.gameModel.Renderable;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jswing.Texture;
import terrain.TerrainCoordinates;

/**
 * A side-view cave, drawn in one-point perspective: solid rock shows its front face on the front
 * plane, and through the open space between the rock the cave goes back in depth, to its back
 * wall, with the sides of the rock running back to it ({@link CavePerspective}).
 *
 * <p>The rock image is the stone texture, if the game has one, or plain grey. The front faces are
 * painted in square pieces of {@link #PIECE_BLOCKS} blocks, see-through where the cave is open,
 * only those near the player, and the most recent {@link #KEPT_PIECES} are kept; the depth behind
 * them is painted afresh each frame, since it turns as the player moves. Call {@link #update} each
 * frame. The front faces come first, since the renderer draws the first sprite on top.
 */
public final class SideCaveScene implements Iterable<Renderable> {

  /** How many blocks across each painted piece is. */
  static final int PIECE_BLOCKS = 8;

  /** How many painted pieces are kept, the most recently used. */
  static final int KEPT_PIECES = 40;

  /**
   * How bright the back wall is, compared with the stone image: lighter than the rock in front,
   * so open space reads as space and rock as solid mass.
   */
  static final double BACK_WALL_BRIGHTNESS = 0.6;

  /** How bright a rock face is, compared with the stone image. */
  static final double FACE_BRIGHTNESS = 0.3;


  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;
  private static final int PIECE_PX = PIECE_BLOCKS * BLOCK;

  private final SideCave cave;
  private final int viewWidthPx;
  private final int viewHeightPx;
  private final BufferedImage face;
  private final CavePerspective depth;
  private final Map<Long, Piece> pieces = new LinkedHashMap<>(64, 0.75f, true);
  private volatile List<Renderable> shown = List.of();

  /**
   * Creates the scene for a cave.
   *
   * @param cave the cave to draw
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   */
  public SideCaveScene(SideCave cave, int viewWidthPx, int viewHeightPx) {
    this(cave, viewWidthPx, viewHeightPx, stoneImage());
  }

  /**
   * Creates the scene for a cave, drawing its rock with a given image.
   *
   * @param cave the cave to draw
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   * @param stone the rock image, scaled to a block
   */
  SideCaveScene(SideCave cave, int viewWidthPx, int viewHeightPx, BufferedImage stone) {
    this.cave = cave;
    this.viewWidthPx = viewWidthPx;
    this.viewHeightPx = viewHeightPx;
    BufferedImage block = scaled(stone);
    // An olive-green back wall and brown rock, as in the cave's concept picture.
    BufferedImage backWall = shaded(block, BACK_WALL_BRIGHTNESS, new double[] {0.92, 1, 0.68});
    this.face = shaded(block, FACE_BRIGHTNESS, new double[] {1, 0.86, 0.66});
    this.depth = new CavePerspective(cave::isSolid, block, backWall, viewWidthPx, viewHeightPx);
  }

  /**
   * Brings the scene up to date around the player, painting any pieces now in view.
   *
   * @param playerX the player's x position, in pixels; the camera is centred on it
   * @param playerY the player's y position, in pixels
   */
  public void update(double playerX, double playerY) {
    int firstX = Math.floorDiv((int) (playerX - viewWidthPx / 2.0) - BLOCK, PIECE_PX);
    int lastX = Math.floorDiv((int) (playerX + viewWidthPx / 2.0) + BLOCK, PIECE_PX);
    int firstY = Math.floorDiv((int) (playerY - viewHeightPx / 2.0) - BLOCK, PIECE_PX);
    int lastY = Math.floorDiv((int) (playerY + viewHeightPx / 2.0) + BLOCK, PIECE_PX);
    List<Renderable> visible = new ArrayList<>();
    for (int pieceX = firstX; pieceX <= lastX; pieceX++) {
      for (int pieceY = firstY; pieceY <= lastY; pieceY++) {
        long key = (long) pieceX << 32 | (pieceY & 0xFFFFFFFFL);
        Piece piece = pieces.get(key);
        if (piece == null) {
          piece = new Piece(pieceX, pieceY, paintPiece(pieceX, pieceY));
          pieces.put(key, piece);
        }
        visible.add(piece);
      }
    }
    while (pieces.size() > KEPT_PIECES) {
      pieces.remove(pieces.keySet().iterator().next());
    }
    depth.update(playerX, playerY);
    // Last, so it is drawn under the front faces.
    visible.add(depth);
    shown = List.copyOf(visible);
  }

  @Override
  public Iterator<Renderable> iterator() {
    return shown.iterator();
  }

  /** Paints one piece: the front face of each solid block, leaving open space see-through. */
  BufferedImage paintPiece(int pieceX, int pieceY) {
    BufferedImage image = new BufferedImage(PIECE_PX, PIECE_PX, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    try {
      for (int column = 0; column < PIECE_BLOCKS; column++) {
        for (int row = 0; row < PIECE_BLOCKS; row++) {
          if (cave.isSolid(pieceX * PIECE_BLOCKS + column, pieceY * PIECE_BLOCKS + row)) {
            // Image rows run down; block rows run up.
            g.drawImage(face, column * BLOCK, PIECE_PX - (row + 1) * BLOCK, null);
          }
        }
      }
    } finally {
      g.dispose();
    }
    return image;
  }

  /** Returns the depth behind the front faces, for tests. */
  CavePerspective depth() {
    return depth;
  }

  private static BufferedImage stoneImage() {
    try {
      return new TerrainImages().get(TileRenderable.TEXTURE_FOLDER + "stone");
    } catch (RuntimeException e) {
      BufferedImage grey = new BufferedImage(BLOCK, BLOCK, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = grey.createGraphics();
      g.setColor(new Color(120, 118, 112));
      g.fillRect(0, 0, BLOCK, BLOCK);
      g.dispose();
      return grey;
    }
  }

  /** Scales an image to a block, with hard pixel edges. */
  private static BufferedImage scaled(BufferedImage image) {
    BufferedImage block = new BufferedImage(BLOCK, BLOCK, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = block.createGraphics();
    try {
      g.setRenderingHint(
          java.awt.RenderingHints.KEY_INTERPOLATION,
          java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      g.drawImage(image, 0, 0, BLOCK, BLOCK, null);
    } finally {
      g.dispose();
    }
    return block;
  }

  /** Returns a copy of an image with its brightness scaled and tinted, channel by channel. */
  private static BufferedImage shaded(BufferedImage image, double brightness, double[] tint) {
    BufferedImage out =
        new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        int argb = image.getRGB(x, y);
        int red = clamp((argb >> 16 & 0xFF) * brightness * tint[0]);
        int green = clamp((argb >> 8 & 0xFF) * brightness * tint[1]);
        int blue = clamp((argb & 0xFF) * brightness * tint[2]);
        out.setRGB(x, y, 0xFF000000 | red << 16 | green << 8 | blue);
      }
    }
    return out;
  }

  private static int clamp(double value) {
    return (int) Math.max(0, Math.min(255, Math.round(value)));
  }

  /** One painted piece, placed by the game camera's rule: by its middle and its bottom edge. */
  private static final class Piece implements Renderable {

    private static final long serialVersionUID = 1L;

    private final int pieceX;
    private final int pieceY;
    private final transient BufferedImage image;

    /** Made on first use, on the Swing thread. */
    private transient Texture texture;

    Piece(int pieceX, int pieceY, BufferedImage image) {
      this.pieceX = pieceX;
      this.pieceY = pieceY;
      this.image = image;
    }

    @Override
    public Texture getTexture() {
      if (texture == null) {
        texture = new ImageTexture(image);
      }
      return texture;
    }

    @Override
    public int getX() {
      return pieceX * PIECE_PX + PIECE_PX / 2;
    }

    @Override
    public int getY() {
      return pieceY * PIECE_PX;
    }

    @Override
    public int getWidth() {
      return PIECE_PX;
    }

    @Override
    public int getHeight() {
      return PIECE_PX;
    }
  }
}
