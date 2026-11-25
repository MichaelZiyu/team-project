package terrainscene;

import cave.CaveMover;
import game.gameModel.Renderable;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import jswing.Texture;
import terrain.TerrainCoordinates;

/**
 * The depth of a side-view cave, in one-point perspective: the cave's back wall far behind the
 * rock, and the sides of the rock running back to it, every edge pointing at one vanishing point
 * in the middle of the screen.
 *
 * <p>The rock is a slab: each solid block's front face is on the front plane, at full size, and the
 * block runs straight back to the back wall, {@link #BACK_DEPTH_PX} (one block) deeper into the
 * screen. The rock's sides are drawn in true perspective ({@link #scaleAt}): seen by a camera a
 * distance D in front of the front plane, a point d deeper is drawn {@code D / (D + d)} times as
 * far from the vanishing point. D comes from the camera's field of view: the front plane fills the
 * screen's width at {@link #FIELD_OF_VIEW_DEGREES} degrees. Where a block faces open space, the
 * side of it that faces the vanishing point shows: the tops of ledges below it, the undersides of
 * ceilings above it, and the sides of walls left or right of it. Each is a smooth, four-cornered
 * shape from the block's front edge back to the back wall, textured with stone in true perspective
 * ({@link StonePaint}), lit at the front and darker toward the back. As the player moves, the
 * vanishing point moves with them and every side turns, as in a real tunnel. Neighbouring blocks'
 * sides that line up are drawn as one long face, so no seams show between them.
 *
 * <p>The back wall's stone is drawn at the size perspective gives a block that deep, but it is
 * fixed to the world, like the front blocks: it moves with them exactly, pixel for pixel, rather
 * than sliding behind them, so the cave holds together as one piece of rock.
 *
 * <p>This draws only what lies behind the front plane; the front faces are drawn over it, by
 * {@link SideCaveScene}. It is painted afresh each frame ({@link #update}), reaching {@link
 * #BORDER_PX} past the screen's edges. The renderer may draw a frame at any moment from its own
 * thread, so each frame is painted into a spare image ({@link #FRAME_IMAGES} take turns) and only
 * then handed over, together with where it goes: the renderer always gets a finished picture and
 * the position it was painted for, never one without the other, so it never shows out of step
 * with the front blocks.
 */
final class CavePerspective implements Renderable {

  private static final long serialVersionUID = 1L;

  /** How far behind the front plane the back wall is, in pixels: one block. */
  static final double BACK_DEPTH_PX = 64;

  /** How wide the camera sees, across the screen, in degrees. */
  static final double FIELD_OF_VIEW_DEGREES = 90;


  /** How far the overlay reaches past each screen edge, in pixels. */
  static final int BORDER_PX = 96;

  /** How many images take turns, so one being shown is never painted over. */
  static final int FRAME_IMAGES = 3;

  /** How the rock's sides are lit: the stone's brightness at each side's front and back edges. */
  enum Lighting {
    /** The top of a ledge, facing up: the brightest. */
    TOP(1.4, 0.56),
    /** The underside of a ceiling, facing down: the darkest. */
    UNDERSIDE(0.77, 0.32),
    /** The side of a wall, facing left or right. */
    SIDE(1.12, 0.43);

    private final double front;
    private final double back;

    Lighting(double front, double back) {
      this.front = front;
      this.back = back;
    }
  }

  /** How much of the stone's red, green and blue the rock's sides show: a warm brown. */
  static final double[] SIDE_TINT = {1, 0.8, 0.5};

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  /** A painted frame, handed to the renderer: which image, and where it goes. */
  private record Frame(int image, int centreX, int bottomY) {}

  /** One side of rock that shows: its front edge's ends on the front plane, and how it is lit. */
  private record Face(
      double x1, double y1, double x2, double y2, Lighting lighting, double distance) {}

  private final CaveMover.Rock cave;
  private final BufferedImage backWall;

  /** The stone image's pixels, ARGB, for texturing the rock's sides. */
  private final int[] stone;

  /** How many pixels across the stone image is. */
  private final int stoneSize;

  /** How far in front of the front plane the camera is, in pixels. */
  private final double cameraDistance;

  /** How large the back wall is drawn, compared with the front plane. */
  private final double backScale;

  /**
   * How large one block of the back wall is drawn, in whole pixels: a block's size at the back
   * wall's scale. Whole pixels keep the back wall cheap to fill each frame.
   */
  private final int backTilePx;
  private final int width;
  private final int height;
  private final BufferedImage[] images = new BufferedImage[FRAME_IMAGES];

  /** Each image's texture, made on first use and never saved. */
  private final transient Texture[] textures = new Texture[FRAME_IMAGES];

  private int nextImage;

  /** The frame last finished, for the renderer. */
  private volatile Frame published = new Frame(0, 0, 0);

  /** The frame the renderer is placing now: the one it took its texture from. */
  private Frame served = published;

  /**
   * Creates the perspective for a cave and a screen of a given size.
   *
   * @param cave where the cave's rock is
   * @param stone the stone image for one block, square, for the rock's sides
   * @param backWall the back wall's image for one block, at full size
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   */
  CavePerspective(
      CaveMover.Rock cave,
      BufferedImage stone,
      BufferedImage backWall,
      int viewWidthPx,
      int viewHeightPx) {
    this.cave = cave;
    this.stoneSize = stone.getWidth();
    this.stone = stone.getRGB(0, 0, stoneSize, stoneSize, null, 0, stoneSize);
    this.cameraDistance = cameraDistance(viewWidthPx);
    this.backScale = scaleAt(BACK_DEPTH_PX, viewWidthPx);
    this.backTilePx = (int) Math.round(BLOCK * backScale);
    this.backWall = shrunk(backWall, backTilePx);
    this.width = viewWidthPx + 2 * BORDER_PX;
    this.height = viewHeightPx + 2 * BORDER_PX;
    for (int i = 0; i < FRAME_IMAGES; i++) {
      images[i] = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }
  }

  /**
   * Paints the back wall and the sides of the rock for a camera position; the vanishing point is
   * the camera, in the middle of the screen.
   *
   * @param cameraX the camera's x position, in world pixels
   * @param cameraY the camera's y position, in world pixels
   */
  void update(double cameraX, double cameraY) {
    // The renderer places sprites relative to the camera's whole-pixel position.
    int camX = (int) cameraX;
    int camY = (int) cameraY;
    int index = nextImage;
    nextImage = (nextImage + 1) % FRAME_IMAGES;
    Graphics2D g = images[index].createGraphics();
    try {
      // The back wall covers the whole image, replacing last frame.
      g.setComposite(AlphaComposite.Src);
      paintBackWall(g, camX, camY);
      g.setComposite(AlphaComposite.SrcOver);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      for (Face face : visibleFaces(camX, camY)) {
        paintFace(g, face, camX, camY);
      }
    } finally {
      g.dispose();
    }
    // Only now, finished, does the frame go to the renderer, with the camera it was painted for.
    published = new Frame(index, camX, camY - height / 2);
  }

  /**
   * Paints the back wall over the whole image: its stone at the size perspective gives a block one
   * block deep, but fixed to the world like the front blocks, so it moves with them exactly.
   */
  private void paintBackWall(Graphics2D g, int camX, int camY) {
    // One block's image, already shrunk, repeated across the image from where the front block at
    // (0, 0) lands, so the pattern is pinned to the world as the camera moves.
    Rectangle2D anchor =
        new Rectangle2D.Double(
            screenX(0, camX, 1), screenY(BLOCK, camY, 1), backTilePx, backTilePx);
    g.setPaint(new TexturePaint(backWall, anchor));
    g.fillRect(0, 0, width, height);
  }

  /**
   * Returns the sides of rock that show from this camera: each run of block sides facing open
   * space and the vanishing point, joined into one face, furthest from the vanishing point first,
   * since those nearer it are in front of them where they overlap.
   */
  private List<Face> visibleFaces(int camX, int camY) {
    List<Face> faces = new ArrayList<>();
    int firstX = Math.floorDiv(camX - width / 2, BLOCK) - 1;
    int lastX = Math.floorDiv(camX + width / 2, BLOCK) + 1;
    int firstY = Math.floorDiv(camY - height / 2, BLOCK) - 1;
    int lastY = Math.floorDiv(camY + height / 2, BLOCK) + 1;
    for (int blockY = firstY; blockY <= lastY; blockY++) {
      int top = (blockY + 1) * BLOCK;
      int bottom = blockY * BLOCK;
      if (top < camY) {
        addRows(faces, blockY, 1, top, firstX, lastX, Lighting.TOP, camX, camY);
      }
      if (bottom > camY) {
        addRows(faces, blockY, -1, bottom, firstX, lastX, Lighting.UNDERSIDE, camX, camY);
      }
    }
    for (int blockX = firstX; blockX <= lastX; blockX++) {
      int right = (blockX + 1) * BLOCK;
      int left = blockX * BLOCK;
      if (right < camX) {
        addColumns(faces, blockX, 1, right, firstY, lastY, camX, camY);
      }
      if (left > camX) {
        addColumns(faces, blockX, -1, left, firstY, lastY, camX, camY);
      }
    }
    faces.sort(Comparator.comparingDouble(Face::distance).reversed());
    return faces;
  }

  /**
   * Adds the runs of block tops (step 1) or undersides (step -1) along a row that face open space,
   * each run as one face along the line {@code y} on the front plane.
   */
  private void addRows(List<Face> faces, int blockY, int step, int y, int firstX, int lastX,
      Lighting lighting, int camX, int camY) {
    int start = Integer.MIN_VALUE;
    for (int blockX = firstX; blockX <= lastX + 1; blockX++) {
      boolean shows =
          blockX <= lastX && cave.isSolid(blockX, blockY) && !cave.isSolid(blockX, blockY + step);
      if (shows && start == Integer.MIN_VALUE) {
        start = blockX;
      } else if (!shows && start != Integer.MIN_VALUE) {
        faces.add(face(start * BLOCK, y, blockX * BLOCK, y, lighting, camX, camY));
        start = Integer.MIN_VALUE;
      }
    }
  }

  /**
   * Adds the runs of block sides facing right (step 1) or left (step -1) up a column that face
   * open space, each run as one face along the line {@code x} on the front plane.
   */
  private void addColumns(List<Face> faces, int blockX, int step, int x, int firstY, int lastY,
      int camX, int camY) {
    int start = Integer.MIN_VALUE;
    for (int blockY = firstY; blockY <= lastY + 1; blockY++) {
      boolean shows =
          blockY <= lastY && cave.isSolid(blockX, blockY) && !cave.isSolid(blockX + step, blockY);
      if (shows && start == Integer.MIN_VALUE) {
        start = blockY;
      } else if (!shows && start != Integer.MIN_VALUE) {
        faces.add(face(x, start * BLOCK, x, blockY * BLOCK, Lighting.SIDE, camX, camY));
        start = Integer.MIN_VALUE;
      }
    }
  }

  private static Face face(
      double x1, double y1, double x2, double y2, Lighting lighting, int camX, int camY) {
    // How close the face's front edge comes to the vanishing point.
    double nearestX = Math.max(Math.min(x1, x2), Math.min(Math.max(x1, x2), camX));
    double nearestY = Math.max(Math.min(y1, y2), Math.min(Math.max(y1, y2), camY));
    double distance = Math.hypot(nearestX - camX, nearestY - camY);
    return new Face(x1, y1, x2, y2, lighting, distance);
  }

  /**
   * Paints one side: the shape from its front edge back to the back wall, filled with stone in
   * perspective, lit at the front and darker at the back ({@link StonePaint}).
   */
  private void paintFace(Graphics2D g, Face face, int camX, int camY) {
    double frontX1 = screenX(face.x1(), camX, 1);
    double frontY1 = screenY(face.y1(), camY, 1);
    double frontX2 = screenX(face.x2(), camX, 1);
    double frontY2 = screenY(face.y2(), camY, 1);
    double backX1 = screenX(face.x1(), camX, backScale);
    double backY1 = screenY(face.y1(), camY, backScale);
    double backX2 = screenX(face.x2(), camX, backScale);
    double backY2 = screenY(face.y2(), camY, backScale);
    Path2D.Double shape = new Path2D.Double();
    shape.moveTo(frontX1, frontY1);
    shape.lineTo(frontX2, frontY2);
    shape.lineTo(backX2, backY2);
    shape.lineTo(backX1, backY1);
    shape.closePath();
    boolean row = face.y1() == face.y2();
    g.setPaint(
        new StonePaint(
            stone,
            stoneSize,
            row,
            row ? face.y1() : face.x1(),
            camX,
            camY,
            width / 2.0,
            height / 2.0,
            cameraDistance,
            BACK_DEPTH_PX,
            face.lighting().front,
            face.lighting().back,
            SIDE_TINT));
    g.fill(shape);
  }

  /**
   * Returns how large a plane is drawn, compared with the front plane, some distance deeper into
   * the screen: by perspective, {@code D / (D + depth)}, for a camera a distance D in front of the
   * front plane. D is where the front plane exactly fills the screen's width at the field of view:
   * {@code D = (width / 2) / tan(fieldOfView / 2)}.
   *
   * @param depthPx how far behind the front plane the plane is, in pixels
   * @param viewWidthPx the screen's width, in pixels
   * @return the scale: 1 on the front plane, smaller further back
   */
  static double scaleAt(double depthPx, int viewWidthPx) {
    double distance = cameraDistance(viewWidthPx);
    return distance / (distance + depthPx);
  }

  /**
   * Returns how far in front of the front plane the camera is, for the front plane to fill the
   * screen's width at the field of view.
   *
   * @param viewWidthPx the screen's width, in pixels
   * @return the distance, in pixels
   */
  static double cameraDistance(int viewWidthPx) {
    return viewWidthPx / 2.0 / Math.tan(Math.toRadians(FIELD_OF_VIEW_DEGREES) / 2);
  }

  /**
   * Returns how large the back wall is drawn, compared with the front plane.
   *
   * @return the back wall's scale
   */
  double backScale() {
    return backScale;
  }

  /**
   * Returns how large one block of the back wall is drawn, in whole pixels.
   *
   * @return the back wall's block size
   */
  int backTilePx() {
    return backTilePx;
  }

  /** Returns an image shrunk smoothly to a square of a given size. */
  private static BufferedImage shrunk(BufferedImage image, int size) {
    BufferedImage small = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = small.createGraphics();
    try {
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g.drawImage(image, 0, 0, size, size, null);
    } finally {
      g.dispose();
    }
    return small;
  }

  /** Returns where a world x lands in the image, at a plane drawn at a given scale. */
  private double screenX(double worldX, int camX, double scale) {
    return width / 2.0 + (worldX - camX) * scale;
  }

  /** Returns where a world y lands in the image (image y points down), at a given scale. */
  private double screenY(double worldY, int camY, double scale) {
    return height / 2.0 - (worldY - camY) * scale;
  }

  /** Returns the image last painted, for tests. */
  BufferedImage image() {
    return images[published.image()];
  }

  /**
   * Returns the latest finished frame's picture, and remembers that frame, so the position the
   * renderer asks for next ({@link #getX}, {@link #getY}) is the one this picture was painted for.
   */
  @Override
  public Texture getTexture() {
    served = published;
    int index = served.image();
    if (textures[index] == null) {
      textures[index] = new ImageTexture(images[index]);
    }
    return textures[index];
  }

  @Override
  public int getX() {
    return served.centreX();
  }

  @Override
  public int getY() {
    return served.bottomY();
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
