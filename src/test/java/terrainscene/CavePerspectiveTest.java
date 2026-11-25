package terrainscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cave.CaveMover;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class CavePerspectiveTest {

  private static final int VIEW_WIDTH = 800;
  private static final int VIEW_HEIGHT = 600;
  private static final int BORDER = CavePerspective.BORDER_PX;
  private static final int BLUE = new Color(0, 0, 255).getRGB();

  /** A back wall of plain blue, so it is easy to tell from the rock's sides. */
  private static BufferedImage blueWall() {
    BufferedImage wall = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = wall.createGraphics();
    g.setColor(new Color(BLUE));
    g.fillRect(0, 0, 64, 64);
    g.dispose();
    return wall;
  }

  /** How large the back wall is drawn for the test screen. */
  private static final double SCALE = CavePerspective.scaleAt(64, VIEW_WIDTH);

  /** Plain grey stone for the rock's sides. */
  private static BufferedImage greyStone() {
    BufferedImage stone = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = stone.createGraphics();
    g.setColor(new Color(120, 120, 120));
    g.fillRect(0, 0, 16, 16);
    g.dispose();
    return stone;
  }

  /** Paints the depth of some rock seen from a camera, and returns the image. */
  private static BufferedImage paint(CaveMover.Rock rock, double cameraX, double cameraY) {
    CavePerspective depth =
        new CavePerspective(rock, greyStone(), blueWall(), VIEW_WIDTH, VIEW_HEIGHT);
    depth.update(cameraX, cameraY);
    return depth.image();
  }

  /** Returns the colour where a point on the screen (from its top-left) lands in the image. */
  private static int at(BufferedImage image, double screenX, double screenY) {
    return image.getRGB((int) Math.round(screenX) + BORDER, (int) Math.round(screenY) + BORDER);
  }

  private static boolean isRockSide(int argb) {
    int red = argb >> 16 & 0xFF;
    int blue = argb & 0xFF;
    return red > blue;
  }

  @Test
  void theBackWallOneBlockDeepIsScaledByTruePerspective() {
    // A 90-degree view across 800 px puts the camera 400 px in front of the front plane, so a
    // plane 64 px deeper is drawn 400 / 464 as large.
    assertEquals(400.0 / 464, SCALE, 1e-9);
    CavePerspective depth =
        new CavePerspective((x, y) -> false, greyStone(), blueWall(), 800, 600);
    assertEquals(400.0 / 464, depth.backScale(), 1e-9);
    assertEquals(Math.round(64 * 400.0 / 464), depth.backTilePx());
  }

  @Test
  void deeperPlanesAreDrawnSmallerAndTheFrontPlaneFullSize() {
    assertEquals(1, CavePerspective.scaleAt(0, VIEW_WIDTH), 1e-12);
    assertTrue(CavePerspective.scaleAt(128, VIEW_WIDTH) < CavePerspective.scaleAt(64, VIEW_WIDTH));
  }

  @Test
  void openSpaceShowsTheBackWall() {
    BufferedImage image = paint((x, y) -> y <= 0, 32, 200);
    assertEquals(BLUE, at(image, VIEW_WIDTH / 2.0, VIEW_HEIGHT / 2.0));
  }

  @Test
  void aFloorBelowEyeLevelShowsItsTopRunningBackToTheWall() {
    // The floor's top edge is 136 px below the camera; at the back wall, 136 * SCALE.
    BufferedImage image = paint((x, y) -> y <= 0, 32, 200);
    double front = VIEW_HEIGHT / 2.0 + 136;
    double back = VIEW_HEIGHT / 2.0 + 136 * SCALE;
    assertTrue(isRockSide(at(image, VIEW_WIDTH / 2.0, (front + back) / 2)));
    assertEquals(BLUE, at(image, VIEW_WIDTH / 2.0, back - 4));
  }

  @Test
  void aCeilingAboveEyeLevelShowsItsUnderside() {
    // The ceiling's underside is 120 px above the camera.
    BufferedImage image = paint((x, y) -> y <= 0 || y >= 5, 32, 200);
    double front = VIEW_HEIGHT / 2.0 - 120;
    double back = VIEW_HEIGHT / 2.0 - 120 * SCALE;
    assertTrue(isRockSide(at(image, VIEW_WIDTH / 2.0, (front + back) / 2)));
  }

  @Test
  void aWallToOneSideShowsTheSideFacingTheCamera() {
    // A wall from x = 256 rightward: its left side faces the camera, 224 px to its left.
    BufferedImage image = paint((x, y) -> y <= 0 || x >= 4, 32, 200);
    double front = VIEW_WIDTH / 2.0 + 224;
    double back = VIEW_WIDTH / 2.0 + 224 * SCALE;
    assertTrue(isRockSide(at(image, (front + back) / 2, VIEW_HEIGHT / 2.0)));
  }

  @Test
  void sidesFacingAwayFromTheCameraDoNotShow() {
    // A ledge above the camera shows only its underside: its top faces away.
    BufferedImage image = paint((x, y) -> y <= 0 || y == 5, 32, 200);
    double ledgeTop = VIEW_HEIGHT / 2.0 - (6 * 64 - 200);
    double towardBack = VIEW_HEIGHT / 2.0 - (6 * 64 - 200) * SCALE;
    // Just behind the ledge's top edge, toward the vanishing point: still the ledge's own front
    // face on screen, which this layer does not draw, so what shows there is the back wall.
    assertEquals(BLUE, at(image, VIEW_WIDTH / 2.0, ledgeTop - 6));
    assertTrue(towardBack > ledgeTop);
  }

  @Test
  void theBackWallMovesWithTheFrontBlocksExactly() {
    // A wall image with one dark column, to follow.
    BufferedImage marked = blueWall();
    for (int y = 0; y < 64; y++) {
      marked.setRGB(0, y, Color.BLACK.getRGB());
    }
    CavePerspective depth =
        new CavePerspective((x, y) -> false, greyStone(), marked, VIEW_WIDTH, VIEW_HEIGHT);
    depth.update(0, 0);
    int before = firstDarkColumn(depth.image());
    depth.update(10, 0);
    int after = firstDarkColumn(depth.image());
    // The camera moved 10 px right, so everything fixed to the world moved 10 px left.
    assertEquals(10, Math.floorMod(before - after, depth.backTilePx()));
  }

  @Test
  void theRendererGetsEachPictureWithThePositionItWasPaintedFor() {
    CavePerspective depth =
        new CavePerspective((x, y) -> false, greyStone(), blueWall(), VIEW_WIDTH, VIEW_HEIGHT);
    depth.update(100, 50);
    jswing.Texture first = depth.getTexture();
    // A new frame finishes while the renderer is between asking for the picture and its place.
    depth.update(300, 70);
    assertEquals(100, depth.getX(), "still the first frame's place");
    jswing.Texture second = depth.getTexture();
    assertEquals(300, depth.getX());
    assertTrue(first != second, "a new frame is a different picture");
  }

  @Test
  void aShownPictureIsNotPaintedOverByTheNextFrames() {
    CavePerspective depth =
        new CavePerspective((x, y) -> y <= 0, greyStone(), blueWall(), VIEW_WIDTH, VIEW_HEIGHT);
    depth.update(32, 200);
    BufferedImage shown = depth.image();
    int[] before = shown.getRGB(0, 0, shown.getWidth(), shown.getHeight(), null, 0,
        shown.getWidth());
    for (int i = 1; i < CavePerspective.FRAME_IMAGES; i++) {
      depth.update(32 + 40 * i, 200);
    }
    int[] after = shown.getRGB(0, 0, shown.getWidth(), shown.getHeight(), null, 0,
        shown.getWidth());
    assertTrue(java.util.Arrays.equals(before, after));
  }

  private static int firstDarkColumn(BufferedImage image) {
    int row = image.getHeight() / 2;
    for (int x = 0; x < image.getWidth(); x++) {
      if ((image.getRGB(x, row) & 0xFF) < 128) {
        return x;
      }
    }
    return -1;
  }
}
