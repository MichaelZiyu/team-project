package terrainscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.Test;
import terrain.BlockType;

class TileRenderableTest {

  private static final int STEP = TileRenderable.STEP_HEIGHT_PX;

  @Test
  void flatTileMatchesTheRenderersCentreAndBottomConvention() {
    TileRenderable tile = new TileRenderable(2, -1, BlockType.GRASS, 0);
    assertEquals(2 * 64 + 32, tile.getX());
    assertEquals(-64, tile.getY());
    assertEquals(64, tile.getWidth());
    assertEquals(64, tile.getHeight());
  }

  @Test
  void raisedTileIsLiftedByItsHeight() {
    TileRenderable tile = new TileRenderable(0, 3, BlockType.STONE, 5);
    assertEquals(3 * 64 + 5 * STEP, tile.getY());
    assertEquals(5, tile.getTerrainHeight());
  }

  @Test
  void faceStepSitsBelowItsTile() {
    FaceRenderable face = new FaceRenderable(2, 3, BlockType.STONE, 4, true);
    assertEquals(2 * 64 + 32, face.getX());
    assertEquals(3 * 64 + 4 * STEP, face.getY());
    assertEquals(64, face.getWidth());
    assertEquals(STEP, face.getHeight());
    // The top step's upper edge meets the bottom edge of a tile at height 5.
    TileRenderable tile = new TileRenderable(2, 3, BlockType.STONE, 5);
    assertEquals(tile.getY(), face.getY() + face.getHeight());
  }

  @Test
  void everyBlockTypeAndTheFaceHaveAPlaceholderImage() {
    for (BlockType type : BlockType.values()) {
      TileRenderable tile = new TileRenderable(0, 0, type, 1);
      assertNotNull(tile.getTexture().getContent().getIcon(), "no image for " + type);
      assertEquals(64, tile.getTexture().getWidth());
      assertEquals(64, tile.getTexture().getHeight());
    }
    FaceRenderable plain = new FaceRenderable(0, 0, BlockType.GRASS, 0, false);
    assertEquals(64, plain.getTexture().getWidth());
    assertEquals(STEP, plain.getTexture().getHeight());
    for (BlockType type : BlockType.values()) {
      FaceRenderable top = new FaceRenderable(0, 0, type, 0, true);
      assertEquals(64, top.getTexture().getWidth(), "no top-step image for " + type);
      assertEquals(STEP, top.getTexture().getHeight());
    }
  }

  @Test
  void shadowCoversItsTileTop() {
    ShadowRenderable shadow =
        new ShadowRenderable(2, 3, 4, new Area(new Rectangle2D.Double(40, 0, 24, 64)));
    TileRenderable tile = new TileRenderable(2, 3, BlockType.GRASS, 4);
    assertEquals(tile.getX(), shadow.getX());
    assertEquals(tile.getY(), shadow.getY());
    assertEquals(64, shadow.getWidth());
    assertEquals(64, shadow.getHeight());
  }

  @Test
  void shadowImageIsDarkOnlyInsideItsShape() {
    // A band along the tile's top (y up), which is the top rows of the image (y down).
    ShadowRenderable shadow =
        new ShadowRenderable(0, 0, 1, new Area(new Rectangle2D.Double(0, 48, 64, 16)));
    BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    shadow.paintOnto(g, 0, 0, null);
    g.dispose();
    assertEquals(ShadowRenderable.SHADOW_COLOUR.getAlpha(), image.getRGB(10, 5) >>> 24);
    assertEquals(0, image.getRGB(10, 40) >>> 24);
    assertEquals(64, shadow.getTexture().getWidth());
  }

  @Test
  void softShadowIsDarkestWhereAllLayersOverlapAndFainterWhereFewDo() {
    Area band = new Area(new Rectangle2D.Double(0, 48, 64, 16));
    Area wider = new Area(new Rectangle2D.Double(0, 32, 64, 32));
    ShadowRenderable shadow = new ShadowRenderable(0, 0, 1, List.of(band, band, wider), 1);
    BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    shadow.paintOnto(g, 0, 0, null);
    g.dispose();
    int full = image.getRGB(10, 5) >>> 24;
    int faint = image.getRGB(10, 24) >>> 24;
    assertTrue(Math.abs(full - ShadowRenderable.SHADOW_COLOUR.getAlpha()) <= 2, "all layers");
    assertTrue(faint > 0 && faint < full, "one layer of three is fainter");
    assertEquals(0, image.getRGB(10, 40) >>> 24);
    assertTrue(shadow.getShape().equals(wider));
    assertThrows(IllegalArgumentException.class, () -> new ShadowRenderable(0, 0, 1, List.of(), 1));
  }

  @Test
  void sunlightCoversItsTileTop() {
    SunlightRenderable sunlight = new SunlightRenderable(2, 3, 4, 1);
    TileRenderable tile = new TileRenderable(2, 3, BlockType.GRASS, 4);
    assertEquals(tile.getX(), sunlight.getX());
    assertEquals(tile.getY(), sunlight.getY());
    assertEquals(64, sunlight.getWidth());
    assertEquals(64, sunlight.getHeight());
    assertEquals(64, sunlight.getTexture().getWidth());
    assertEquals(64, sunlight.getTexture().getHeight());
  }

  @Test
  void sunlightAndShadowsFadeWithTheirStrength() {
    BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    new SunlightRenderable(0, 0, 1, 0.5).paintOnto(g, 0, 0, null);
    g.dispose();
    int half = SunlightRenderable.SUNLIGHT_COLOUR.getAlpha() / 2;
    assertTrue(Math.abs((image.getRGB(5, 5) >>> 24) - half) <= 1);
    Area all = new Area(new Rectangle2D.Double(0, 0, 64, 64));
    image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
    g = image.createGraphics();
    new ShadowRenderable(0, 0, 1, List.of(all), 0.5).paintOnto(g, 0, 0, null);
    g.dispose();
    int fullAlpha = ShadowRenderable.SHADOW_COLOUR.getAlpha();
    assertTrue(Math.abs((image.getRGB(5, 5) >>> 24) - fullAlpha / 2) <= 1);
    assertThrows(IllegalArgumentException.class, () -> new SunlightRenderable(0, 0, 1, 2));
    assertThrows(
        IllegalArgumentException.class, () -> new ShadowRenderable(0, 0, 1, List.of(all), -1));
  }

  @Test
  void cornerOcclusionSitsInItsBackCorner() {
    int left = 2 * 64;
    int bottom = 3 * 64 + 4 * STEP;
    int size = CornerOcclusionRenderable.SIZE_PX;
    CornerOcclusionRenderable backLeft =
        new CornerOcclusionRenderable(2, 3, 4, Corner.BACK_LEFT);
    assertEquals(left, backLeft.getX() - backLeft.getWidth() / 2);
    assertEquals(bottom + 64 - size, backLeft.getY());
    CornerOcclusionRenderable backRight =
        new CornerOcclusionRenderable(2, 3, 4, Corner.BACK_RIGHT);
    assertEquals(left + 64 - size, backRight.getX() - backRight.getWidth() / 2);
    for (Corner corner : Corner.values()) {
      CornerOcclusionRenderable occlusion = new CornerOcclusionRenderable(0, 0, 1, corner);
      assertEquals(size, occlusion.getTexture().getWidth(), corner.name());
      assertEquals(size, occlusion.getTexture().getHeight(), corner.name());
    }
  }

  @Test
  void rimsRunAlongTheRightEdgeOfTheTileTop() {
    int left = 2 * 64;
    int bottom = 3 * 64 + 4 * STEP;
    int thick = RimRenderable.THICKNESS_PX;
    RimRenderable back = new RimRenderable(2, 3, BlockType.GRASS, 4, Edge.BACK, 0);
    assertEquals(left + 32, back.getX());
    assertEquals(bottom + 64 - thick, back.getY());
    assertEquals(64, back.getWidth());
    assertEquals(thick, back.getHeight());
    RimRenderable front = new RimRenderable(2, 3, BlockType.GRASS, 4, Edge.FRONT, 0);
    assertEquals(left + 32, front.getX());
    assertEquals(bottom, front.getY());
    assertEquals(64, front.getWidth());
    assertEquals(thick, front.getHeight());
    RimRenderable leftRim = new RimRenderable(2, 3, BlockType.GRASS, 4, Edge.LEFT, 1);
    // The renderer places a sprite at getX() - width / 2, which must be the tile's left edge.
    assertEquals(left, leftRim.getX() - leftRim.getWidth() / 2);
    assertEquals(bottom, leftRim.getY());
    assertEquals(64, leftRim.getHeight());
    RimRenderable rightRim =
        new RimRenderable(2, 3, BlockType.GRASS, 4, Edge.RIGHT, 0.5);
    assertEquals(left + 64 - thick, rightRim.getX() - rightRim.getWidth() / 2);
  }

  @Test
  void everyBlockTypeHasRimImagesForEverySide() {
    for (BlockType type : BlockType.values()) {
      for (Edge side : Edge.values()) {
        for (double light : new double[] {0, 0.5, 1}) {
          RimRenderable rim = new RimRenderable(0, 0, type, 1, side, light);
          String name = type + " " + side + " at light " + light;
          assertEquals(rim.getWidth(), rim.getTexture().getWidth(), name);
          assertEquals(rim.getHeight(), rim.getTexture().getHeight(), name);
        }
      }
    }
  }

  @Test
  void partLitRimBlendsBetweenTheDarkAndBrightImages() {
    TerrainImages images = new TerrainImages();
    int dark = images.get(RimRenderable.TEXTURE_FOLDER + "grass/right").getRGB(1, 1);
    int bright = images.get(RimRenderable.LIT_TEXTURE_FOLDER + "grass/right").getRGB(1, 1);
    BufferedImage image = new BufferedImage(3, 64, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    new RimRenderable(0, 0, BlockType.GRASS, 1, Edge.RIGHT, 0.5).paintOnto(g, 0, 0, images);
    g.dispose();
    int blended = image.getRGB(1, 1);
    for (int shift = 0; shift <= 16; shift += 8) {
      int expected = (((dark >> shift) & 0xFF) + ((bright >> shift) & 0xFF)) / 2;
      assertTrue(Math.abs(((blended >> shift) & 0xFF) - expected) <= 1, "halfway at " + shift);
    }
    assertThrows(
        IllegalArgumentException.class,
        () -> new RimRenderable(0, 0, BlockType.GRASS, 1, Edge.RIGHT, 1.5));
  }

  @Test
  void occlusionHugsTheEdgeAgainstTheWall() {
    int left = 2 * 64;
    int bottom = 3 * 64 + 4 * STEP;
    int thick = OcclusionRenderable.THICKNESS_PX;
    OcclusionRenderable back = new OcclusionRenderable(2, 3, 4, Edge.BACK);
    assertEquals(left + 32, back.getX());
    assertEquals(bottom + 64 - thick, back.getY());
    assertEquals(64, back.getWidth());
    assertEquals(thick, back.getHeight());
    OcclusionRenderable leftEdge = new OcclusionRenderable(2, 3, 4, Edge.LEFT);
    assertEquals(left, leftEdge.getX() - leftEdge.getWidth() / 2);
    assertEquals(bottom, leftEdge.getY());
    OcclusionRenderable rightEdge = new OcclusionRenderable(2, 3, 4, Edge.RIGHT);
    assertEquals(left + 64 - thick, rightEdge.getX() - rightEdge.getWidth() / 2);
    for (Edge edge : new Edge[] {Edge.BACK, Edge.LEFT, Edge.RIGHT}) {
      OcclusionRenderable occlusion = new OcclusionRenderable(0, 0, 1, edge);
      assertEquals(occlusion.getWidth(), occlusion.getTexture().getWidth(), edge.name());
      assertEquals(occlusion.getHeight(), occlusion.getTexture().getHeight(), edge.name());
    }
    assertThrows(IllegalArgumentException.class, () -> new OcclusionRenderable(0, 0, 1, Edge.FRONT));
  }

  @Test
  void faceShadeCoversExactlyItsFaceStep() {
    FaceShadeRenderable shade = new FaceShadeRenderable(2, 3, 4);
    FaceRenderable face = new FaceRenderable(2, 3, BlockType.GRASS, 4, true);
    assertEquals(face.getX(), shade.getX());
    assertEquals(face.getY(), shade.getY());
    assertEquals(face.getWidth(), shade.getWidth());
    assertEquals(face.getHeight(), shade.getHeight());
    assertEquals(64, shade.getTexture().getWidth());
    assertEquals(STEP, shade.getTexture().getHeight());
  }

  @Test
  void eachTileHasItsOwnLabel() {
    // A Swing label can only be in one place, so two grass tiles must not share one.
    TileRenderable first = new TileRenderable(0, 0, BlockType.GRASS, 1);
    TileRenderable second = new TileRenderable(1, 0, BlockType.GRASS, 1);
    assertNotSame(first.getTexture().getContent(), second.getTexture().getContent());
  }
}
