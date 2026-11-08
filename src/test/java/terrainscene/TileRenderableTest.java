package terrainscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
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
  void rimsRunAlongTheRightEdgeOfTheTileTop() {
    int left = 2 * 64;
    int bottom = 3 * 64 + 4 * STEP;
    int thick = RimRenderable.THICKNESS_PX;
    RimRenderable back = new RimRenderable(2, 3, BlockType.GRASS, 4, Edge.BACK);
    assertEquals(left + 32, back.getX());
    assertEquals(bottom + 64 - thick, back.getY());
    assertEquals(64, back.getWidth());
    assertEquals(thick, back.getHeight());
    RimRenderable front = new RimRenderable(2, 3, BlockType.GRASS, 4, Edge.FRONT);
    assertEquals(left + 32, front.getX());
    assertEquals(bottom, front.getY());
    assertEquals(64, front.getWidth());
    assertEquals(thick, front.getHeight());
    RimRenderable leftRim = new RimRenderable(2, 3, BlockType.GRASS, 4, Edge.LEFT);
    // The renderer places a sprite at getX() - width / 2, which must be the tile's left edge.
    assertEquals(left, leftRim.getX() - leftRim.getWidth() / 2);
    assertEquals(bottom, leftRim.getY());
    assertEquals(64, leftRim.getHeight());
    RimRenderable rightRim =
        new RimRenderable(2, 3, BlockType.GRASS, 4, Edge.RIGHT);
    assertEquals(left + 64 - thick, rightRim.getX() - rightRim.getWidth() / 2);
  }

  @Test
  void everyBlockTypeHasRimImagesForEverySide() {
    for (BlockType type : BlockType.values()) {
      for (Edge side : Edge.values()) {
        RimRenderable rim = new RimRenderable(0, 0, type, 1, side);
        assertEquals(rim.getWidth(), rim.getTexture().getWidth(), type + " " + side);
        assertEquals(rim.getHeight(), rim.getTexture().getHeight(), type + " " + side);
      }
    }
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
