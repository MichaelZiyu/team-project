package terrainscene;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import terrain.TerrainCoordinates;
import terrain.Tree;

/**
 * Paints trees, block by block, the way the terrain is painted: each block shows its top, a
 * square as wide as a tile, and its front face, half as tall, below it, shaded darker. Blocks are
 * painted from the back to the front and from the bottom up, so nearer and higher blocks cover
 * the ones behind and below them.
 *
 * <p>Log and leaf images come from the texture folders under {@link #TEXTURE_FOLDER} if they are
 * there ({@code log-top}, {@code log-side} and {@code leaves}); otherwise from small pixel-art
 * pictures drawn here, scaled up to a block like the terrain's own textures.
 *
 * <p>See-through pixels in the images stay see-through: through gaps in the leaves show the
 * leaves and trunk behind them, and the ground under the tree.
 *
 * <p>Each shape of tree is painted once at each opacity it is shown at, and kept. Safe to call from
 * several threads at once.
 */
public final class TreePainter {

  /** Image folder, under the textures root, holding the tree's own images, if any. */
  static final String TEXTURE_FOLDER = TileRenderable.TEXTURE_FOLDER + "tree/";

  /** How much darker front faces are than tops, as on the terrain. */
  static final double FACE_DARKENING = 0.3;

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;
  private static final int STEP = TileRenderable.STEP_HEIGHT_PX;

  /** How many pixels across the drawn pictures are, before scaling up to a block. */
  private static final int PIXELS = 16;

  private final BufferedImage logTop;
  private final BufferedImage leavesTop;
  /** Front faces: [0] the lower half of the side image, [1] the upper half. */
  private final BufferedImage[] logFaces;

  private final BufferedImage[] leavesFaces;
  private final Map<String, BufferedImage> painted = new ConcurrentHashMap<>();

  /** Creates a painter using the tree images in the game's texture folders, if any. */
  public TreePainter() {
    this(new TerrainImages());
  }

  /**
   * Creates a painter loading tree images through a given loader.
   *
   * @param images loads texture folders
   */
  TreePainter(TerrainImages images) {
    BufferedImage side = blockSized(load(images, "log-side", TreePainter::drawBark), BLOCK);
    // With bark but no log top of its own, the trunk is bark all over: inside the canopy, seen
    // through the leaves, it should look like wood, not cut rings.
    boolean ownBark = hasFolder("log-side");
    this.logTop =
        hasFolder("log-top") || !ownBark
            ? blockSized(load(images, "log-top", TreePainter::drawLogTop), BLOCK)
            : side;
    this.leavesTop = blockSized(load(images, "leaves", TreePainter::drawLeaves), BLOCK);
    this.logFaces = halves(side);
    this.leavesFaces = halves(leavesTop);
  }

  /**
   * Returns how far below the trunk tile's top a tree's picture reaches, and how tall it is:
   * {@code [bottom, height]}, in pixels, where bottom is measured upward from the bottom edge of
   * the trunk tile's top (so it is negative when the canopy reaches out in front).
   *
   * @param shape the tree's shape
   * @return its picture's bottom and height, in pixels
   */
  static int[] bounds(Tree.Shape shape) {
    int bottom = Integer.MAX_VALUE;
    int top = Integer.MIN_VALUE;
    for (Tree.Block block : shape.blocks()) {
      bottom = Math.min(bottom, block.dy() * BLOCK + (block.level() - 1) * STEP);
      top = Math.max(top, block.dy() * BLOCK + block.level() * STEP + BLOCK);
    }
    return new int[] {bottom, top - bottom};
  }

  /**
   * Returns how wide a tree's picture is.
   *
   * @param shape the tree's shape
   * @return its width, in pixels: the canopy's width
   */
  static int width(Tree.Shape shape) {
    return (2 * shape.radius() + 1) * BLOCK;
  }

  /**
   * Returns a picture of a tree of a given shape, at a given opacity.
   *
   * @param shape the tree's shape
   * @param opacity from 0 (invisible) to 1 (solid); rounded to a twentieth
   * @return the picture; never changed afterwards
   */
  public BufferedImage paint(Tree.Shape shape, double opacity) {
    int twentieths = (int) Math.round(Math.max(0, Math.min(1, opacity)) * 20);
    return painted.computeIfAbsent(
        shape + "@" + twentieths, key -> paintAt(shape, twentieths / 20.0));
  }

  private BufferedImage paintAt(Tree.Shape shape, double opacity) {
    if (opacity < 1) {
      BufferedImage solid = paint(shape, 1);
      BufferedImage faded =
          new BufferedImage(solid.getWidth(), solid.getHeight(), BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = faded.createGraphics();
      try {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) opacity));
        g.drawImage(solid, 0, 0, null);
      } finally {
        g.dispose();
      }
      return faded;
    }
    int[] bounds = bounds(shape);
    int top = bounds[0] + bounds[1];
    BufferedImage image =
        new BufferedImage(width(shape), bounds[1], BufferedImage.TYPE_INT_ARGB);
    List<Tree.Block> order = new ArrayList<>(shape.blocks());
    // From the back to the front, then from the bottom up.
    order.sort(
        Comparator.comparingInt((Tree.Block block) -> -block.dy())
            .thenComparingInt(Tree.Block::level));
    Graphics2D g = image.createGraphics();
    try {
      for (Tree.Block block : order) {
        int left = (block.dx() + shape.radius()) * BLOCK;
        int topFace = top - (block.dy() * BLOCK + block.level() * STEP + BLOCK);
        boolean log = block.material() == Tree.Material.LOG;
        g.drawImage(log ? logTop : leavesTop, left, topFace, null);
        // Odd levels show the image's lower half and even levels its upper half, so a column of
        // faces, such as a trunk, shows the image running on unbroken all the way up.
        BufferedImage face = (log ? logFaces : leavesFaces)[1 - block.level() % 2];
        g.drawImage(face, left, topFace + BLOCK, null);
      }
    } finally {
      g.dispose();
    }
    return image;
  }

  /** A picture drawn here when the game has no image of its own. */
  @FunctionalInterface
  private interface Drawing {
    BufferedImage draw();
  }

  private static boolean hasFolder(String name) {
    return Files.isDirectory(TerrainImages.TEXTURES_ROOT.resolve(TEXTURE_FOLDER + name));
  }

  private static BufferedImage load(TerrainImages images, String name, Drawing fallback) {
    String folder = TEXTURE_FOLDER + name;
    if (hasFolder(name)) {
      try {
        return images.get(folder);
      } catch (RuntimeException e) {
        System.err.println("Could not load " + folder + "; drawing it instead: " + e);
      }
    }
    return fallback.draw();
  }

  /** Scales a picture to a block's size with hard pixel edges, like the terrain's textures. */
  private static BufferedImage blockSized(BufferedImage picture, int size) {
    BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    try {
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      g.drawImage(picture, 0, 0, size, size, null);
    } finally {
      g.dispose();
    }
    return image;
  }

  /** Returns a block-sized image's lower and upper halves, darkened for front faces. */
  private static BufferedImage[] halves(BufferedImage image) {
    return new BufferedImage[] {
      darkened(image.getSubimage(0, BLOCK - STEP, BLOCK, STEP)),
      darkened(image.getSubimage(0, 0, BLOCK, STEP))
    };
  }

  /** Returns a copy of a picture with its colours darkened for a front face; holes stay holes. */
  private static BufferedImage darkened(BufferedImage picture) {
    BufferedImage image =
        new BufferedImage(picture.getWidth(), picture.getHeight(), BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < picture.getHeight(); y++) {
      for (int x = 0; x < picture.getWidth(); x++) {
        int argb = picture.getRGB(x, y);
        int red = (int) ((argb >> 16 & 0xFF) * (1 - FACE_DARKENING));
        int green = (int) ((argb >> 8 & 0xFF) * (1 - FACE_DARKENING));
        int blue = (int) ((argb & 0xFF) * (1 - FACE_DARKENING));
        image.setRGB(x, y, argb & 0xFF000000 | red << 16 | green << 8 | blue);
      }
    }
    return image;
  }

  /** Bark: brown, with darker grooves running up it and a few lighter ridges. */
  private static BufferedImage drawBark() {
    Random random = new Random(11);
    BufferedImage image = picture(new Color(104, 78, 46));
    for (int x = 0; x < PIXELS; x++) {
      boolean groove = random.nextInt(3) == 0;
      for (int y = 0; y < PIXELS; y++) {
        if (groove && random.nextInt(6) != 0) {
          image.setRGB(x, y, new Color(74, 54, 31).getRGB());
        } else if (random.nextInt(9) == 0) {
          image.setRGB(x, y, new Color(128, 98, 60).getRGB());
        }
      }
    }
    return image;
  }

  /** A cut log's top: pale rings inside a ring of bark. */
  private static BufferedImage drawLogTop() {
    BufferedImage image = picture(new Color(104, 78, 46));
    Color[] rings = {
      new Color(176, 140, 88), new Color(150, 116, 70), new Color(176, 140, 88),
      new Color(150, 116, 70), new Color(168, 132, 82)
    };
    for (int y = 0; y < PIXELS; y++) {
      for (int x = 0; x < PIXELS; x++) {
        double centreX = x - (PIXELS - 1) / 2.0;
        double centreY = y - (PIXELS - 1) / 2.0;
        int ring = (int) Math.max(Math.abs(centreX), Math.abs(centreY));
        if (ring < rings.length * 1.4) {
          image.setRGB(x, y, rings[Math.min(rings.length - 1, ring / 2)].getRGB());
        }
      }
    }
    return image;
  }

  /** Leaves: green, speckled darker and lighter. */
  private static BufferedImage drawLeaves() {
    Random random = new Random(23);
    BufferedImage image = picture(new Color(64, 122, 44));
    for (int y = 0; y < PIXELS; y++) {
      for (int x = 0; x < PIXELS; x++) {
        int pick = random.nextInt(10);
        if (pick < 3) {
          image.setRGB(x, y, new Color(44, 94, 32).getRGB());
        } else if (pick == 3) {
          image.setRGB(x, y, new Color(92, 152, 62).getRGB());
        } else if (pick == 4) {
          image.setRGB(x, y, new Color(34, 74, 26).getRGB());
        }
      }
    }
    return image;
  }

  private static BufferedImage picture(Color fill) {
    BufferedImage image = new BufferedImage(PIXELS, PIXELS, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    try {
      g.setColor(fill);
      g.fillRect(0, 0, PIXELS, PIXELS);
    } finally {
      g.dispose();
    }
    return image;
  }
}
