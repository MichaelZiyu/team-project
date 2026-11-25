package terrainscene;

import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import terrain.TerrainCoordinates;

/**
 * Paints one side of a rock block, running back from the front plane into the screen, with the
 * stone texture laid on it in true perspective, lit at the front and darker toward the back.
 *
 * <p>The side is a flat strip of rock in the world: along a line on the front plane (a row, at a
 * fixed world y, or a column, at a fixed world x), and {@code depth} pixels back into the screen.
 * A point on it {@code d} deep is drawn at scale {@code D / (D + d)} toward the vanishing point,
 * for a camera {@code D} in front of the front plane ({@link CavePerspective#scaleAt}). This paint
 * runs that the other way: for each pixel on the screen it works out the scale from where the
 * pixel is, then how deep it is and how far along the line, and so which pixel of the stone it
 * shows. The shape it fills sets the side's outline; the paint only colours inside it, so the
 * outline keeps smooth, anti-aliased edges.
 */
final class StonePaint implements Paint {

  /** How wide a block is, in world pixels: one copy of the stone image. */
  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  private final int[] stone;
  private final int stoneSize;
  private final boolean row;
  private final double line;
  private final double cameraX;
  private final double cameraY;
  private final double centreX;
  private final double centreY;
  private final double cameraDistance;
  private final double depth;
  private final double frontBrightness;
  private final double backBrightness;
  private final double[] tint;

  /**
   * Creates the paint for one side.
   *
   * @param stone the stone image's pixels, ARGB, a square {@code stoneSize} across, which covers
   *     one block
   * @param stoneSize how many pixels across the stone image is
   * @param row {@code true} for a ledge's top or a ceiling's underside, along a row at a fixed
   *     world y; {@code false} for a wall's side, up a column at a fixed world x
   * @param line the world y of the row, or the world x of the column, in pixels
   * @param cameraX the camera's world x, in pixels
   * @param cameraY the camera's world y, in pixels
   * @param centreX where the vanishing point is in the image, across, in pixels
   * @param centreY where the vanishing point is in the image, down, in pixels
   * @param cameraDistance how far in front of the front plane the camera is, in pixels
   * @param depth how deep the side runs back into the screen, in pixels
   * @param frontBrightness how bright the stone is at the side's front edge
   * @param backBrightness how bright the stone is at its back edge
   * @param tint how much of the stone's red, green and blue shows
   */
  StonePaint(
      int[] stone,
      int stoneSize,
      boolean row,
      double line,
      double cameraX,
      double cameraY,
      double centreX,
      double centreY,
      double cameraDistance,
      double depth,
      double frontBrightness,
      double backBrightness,
      double[] tint) {
    this.stone = stone;
    this.stoneSize = stoneSize;
    this.row = row;
    this.line = line;
    this.cameraX = cameraX;
    this.cameraY = cameraY;
    this.centreX = centreX;
    this.centreY = centreY;
    this.cameraDistance = cameraDistance;
    this.depth = depth;
    this.frontBrightness = frontBrightness;
    this.backBrightness = backBrightness;
    this.tint = tint;
  }

  /**
   * Returns the colour, as ARGB, that the side shows at a point in the image.
   *
   * @param imageX the point across the image, in pixels
   * @param imageY the point down the image, in pixels
   * @return the shaded stone colour there
   */
  int colourAt(double imageX, double imageY) {
    // The scale this point is drawn at, from how far it is from the vanishing point compared
    // with how far the side's line is from the camera.
    double scale =
        row
            ? (centreY - imageY) / (line - cameraY)
            : (imageX - centreX) / (line - cameraX);
    scale = Math.max(1e-6, Math.min(1, scale));
    double deep = Math.max(0, Math.min(depth - 1e-9, cameraDistance / scale - cameraDistance));
    double along =
        row
            ? cameraX + (imageX - centreX) / scale
            : cameraY + (centreY - imageY) / scale;
    int acrossStone = (int) (deep / depth * stoneSize);
    int alongStone = Math.floorMod((int) Math.floor(along / BLOCK * stoneSize), stoneSize);
    int texel =
        row
            ? stone[acrossStone * stoneSize + alongStone]
            : stone[(stoneSize - 1 - alongStone) * stoneSize + acrossStone];
    double light = frontBrightness + (backBrightness - frontBrightness) * (deep / depth);
    int red = clamp((texel >> 16 & 0xFF) * light * tint[0]);
    int green = clamp((texel >> 8 & 0xFF) * light * tint[1]);
    int blue = clamp((texel & 0xFF) * light * tint[2]);
    return 0xFF000000 | red << 16 | green << 8 | blue;
  }

  private static int clamp(double value) {
    return (int) Math.max(0, Math.min(255, value));
  }

  @Override
  public PaintContext createContext(
      ColorModel model,
      Rectangle deviceBounds,
      Rectangle2D userBounds,
      AffineTransform transform,
      RenderingHints hints) {
    return new PaintContext() {
      @Override
      public void dispose() {}

      @Override
      public ColorModel getColorModel() {
        return ColorModel.getRGBdefault();
      }

      @Override
      public Raster getRaster(int x, int y, int width, int height) {
        WritableRaster raster = getColorModel().createCompatibleWritableRaster(width, height);
        int[] pixels = new int[width * height];
        for (int j = 0; j < height; j++) {
          for (int i = 0; i < width; i++) {
            // Device space is the image's own, so the pixel's middle is at +0.5.
            pixels[j * width + i] = colourAt(x + i + 0.5, y + j + 0.5);
          }
        }
        raster.setDataElements(0, 0, width, height, pixels);
        return raster;
      }
    };
  }

  @Override
  public int getTransparency() {
    return Transparency.OPAQUE;
  }
}
