package terrainscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.gameModel.Renderable;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jswing.Texture;
import org.junit.jupiter.api.Test;
import terrain.BlockType;
import terrain.OverworldTerrain;
import terrain.Terrain;
import terrain.TerrainCoordinates;

class TerrainSceneTest {

  private static final int VIEW_WIDTH = 800;
  private static final int VIEW_HEIGHT = 600;
  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  /** 800 px wide: 7 blocks each side to cover 400 px, plus the margin, plus the centre column. */
  private static final int COLUMNS = 2 * (7 + TerrainScene.MARGIN_BLOCKS) + 1;

  /** 600 px tall: 5 each side plus the margin, the centre row, and the extra rows for hills. */
  private static final int ROWS =
      2 * (5 + TerrainScene.MARGIN_BLOCKS) + 1 + TerrainScene.ROWS_BELOW_FOR_HILLS;

  /** A piece that records what it is without loading any image. */
  private static final class FakePiece implements Renderable {
    private static final long serialVersionUID = 1L;
    private final boolean isFace;
    private final int blockX;
    private final int blockY;
    private final BlockType type;
    private final int value;
    private boolean topStep;
    private boolean isShadow;
    private List<Area> shadowLayers;
    private Edge rimSide;
    private Edge occlusionEdge;
    private Corner occlusionCorner;
    private boolean isSunlight;
    private boolean isFaceShade;

    /** Returns whether this piece lies over its tile's top, rather than being the top or a face. */
    boolean isOverlay() {
      return isShadow
          || rimSide != null
          || occlusionEdge != null
          || occlusionCorner != null
          || isSunlight;
    }

    FakePiece(boolean isFace, int blockX, int blockY, BlockType type, int value) {
      this.isFace = isFace;
      this.blockX = blockX;
      this.blockY = blockY;
      this.type = type;
      this.value = value;
    }

    @Override
    public Texture getTexture() {
      return null;
    }

    @Override
    public int getX() {
      return blockX;
    }

    @Override
    public int getY() {
      return blockY;
    }

    @Override
    public int getWidth() {
      return BLOCK;
    }

    @Override
    public int getHeight() {
      return BLOCK;
    }
  }

  private final Terrain terrain = OverworldTerrain.fromSeed(42L);
  private int piecesCreated = 0;

  private TerrainScene newScene() {
    return new TerrainScene(
        terrain,
        VIEW_WIDTH,
        VIEW_HEIGHT,
        new TerrainScene.TileFactory() {
          @Override
          public Renderable top(int blockX, int blockY, BlockType type, int height) {
            piecesCreated++;
            return new FakePiece(false, blockX, blockY, type, height);
          }

          @Override
          public Renderable face(
              int blockX, int blockY, BlockType tileType, int level, boolean topStep) {
            piecesCreated++;
            FakePiece face = new FakePiece(true, blockX, blockY, tileType, level);
            face.topStep = topStep;
            return face;
          }

          @Override
          public Renderable shadow(int blockX, int blockY, int height, List<Area> layers) {
            piecesCreated++;
            FakePiece shadow = new FakePiece(false, blockX, blockY, null, height);
            shadow.isShadow = true;
            shadow.shadowLayers = layers;
            return shadow;
          }

          @Override
          public Renderable sunlight(int blockX, int blockY, int height) {
            piecesCreated++;
            FakePiece sunlight = new FakePiece(false, blockX, blockY, null, height);
            sunlight.isSunlight = true;
            return sunlight;
          }

          @Override
          public Renderable rim(
              int blockX, int blockY, BlockType type, int height, Edge side) {
            piecesCreated++;
            FakePiece rim = new FakePiece(false, blockX, blockY, type, height);
            rim.rimSide = side;
            return rim;
          }

          @Override
          public Renderable occlusion(int blockX, int blockY, int height, Edge edge) {
            piecesCreated++;
            FakePiece occlusion = new FakePiece(false, blockX, blockY, null, height);
            occlusion.occlusionEdge = edge;
            return occlusion;
          }

          @Override
          public Renderable cornerOcclusion(int blockX, int blockY, int height, Corner corner) {
            piecesCreated++;
            FakePiece occlusion = new FakePiece(false, blockX, blockY, null, height);
            occlusion.occlusionCorner = corner;
            return occlusion;
          }

          @Override
          public Renderable faceShade(int blockX, int blockY, int level) {
            piecesCreated++;
            FakePiece shade = new FakePiece(false, blockX, blockY, null, level);
            shade.isFaceShade = true;
            return shade;
          }
        });
  }

  private static List<FakePiece> pieces(TerrainScene scene) {
    List<FakePiece> pieces = new ArrayList<>();
    for (Renderable piece : scene) {
      pieces.add((FakePiece) piece);
    }
    return pieces;
  }

  private static Map<String, FakePiece> topsByPosition(TerrainScene scene) {
    Map<String, FakePiece> tops = new HashMap<>();
    for (FakePiece piece : pieces(scene)) {
      if (!piece.isFace && !piece.isOverlay() && !piece.isFaceShade) {
        tops.put(piece.blockX + "," + piece.blockY, piece);
      }
    }
    return tops;
  }

  @Test
  void sceneIsEmptyBeforeFirstUpdate() {
    TerrainScene scene = newScene();
    assertEquals(0, scene.size());
    assertEquals(0, scene.tileCount());
  }

  @Test
  void sceneCoversTheScreenPlusMarginAndRowsBelowForHills() {
    TerrainScene scene = newScene();
    scene.update(0, 0);
    assertEquals(COLUMNS * ROWS, scene.tileCount());
    Map<String, FakePiece> tops = topsByPosition(scene);
    assertEquals(COLUMNS * ROWS, tops.size());
    assertTrue(tops.containsKey("-8,6"));
    assertTrue(tops.containsKey("8," + (-6 - TerrainScene.ROWS_BELOW_FOR_HILLS)));
  }

  @Test
  void topsShowTheTerrainTypeAndHeight() {
    TerrainScene scene = newScene();
    scene.update(-3000.5, 4321);
    for (FakePiece top : topsByPosition(scene).values()) {
      assertEquals(terrain.blockAt(top.blockX, top.blockY), top.type);
      assertEquals(terrain.heightAt(top.blockX, top.blockY), top.value);
    }
  }

  @Test
  void eachTileHasOneFacePerLevelAboveTheTileInFront() {
    TerrainScene scene = newScene();
    scene.update(0, 0);
    Map<String, List<Integer>> faceLevels = new HashMap<>();
    for (FakePiece piece : pieces(scene)) {
      if (piece.isFace) {
        faceLevels
            .computeIfAbsent(piece.blockX + "," + piece.blockY, k -> new ArrayList<>())
            .add(piece.value);
      }
    }
    for (FakePiece top : topsByPosition(scene).values()) {
      int height = terrain.heightAt(top.blockX, top.blockY);
      int front = terrain.heightAt(top.blockX, top.blockY - 1);
      List<Integer> expected = new ArrayList<>();
      for (int level = height - 1; level >= front; level--) {
        expected.add(level);
      }
      List<Integer> actual =
          faceLevels.getOrDefault(top.blockX + "," + top.blockY, new ArrayList<>());
      assertEquals(expected, actual, "faces at " + top.blockX + "," + top.blockY);
    }
    assertEquals(scene.size(), pieces(scene).size());
  }

  @Test
  void onlyTheHighestFaceStepIsTheTopStepAndItKnowsTheTileType() {
    TerrainScene scene = newScene();
    scene.update(0, 0);
    int faces = 0;
    for (FakePiece piece : pieces(scene)) {
      if (piece.isFace) {
        faces++;
        int height = terrain.heightAt(piece.blockX, piece.blockY);
        assertEquals(piece.value == height - 1, piece.topStep);
        assertEquals(terrain.blockAt(piece.blockX, piece.blockY), piece.type);
      }
    }
    assertTrue(faces > 0, "expected some terrace faces near spawn");
  }

  @Test
  void shadowsMatchSunShadowsAndComeJustBeforeTheirTile() {
    TerrainScene scene = newScene();
    scene.update(0, 0);
    SunShadows sun = new SunShadows(terrain);
    List<FakePiece> all = pieces(scene);
    int shadowCount = 0;
    for (int i = 0; i < all.size(); i++) {
      FakePiece piece = all.get(i);
      if (piece.isShadow) {
        shadowCount++;
        List<Area> expected = sun.softShadowOn(piece.blockX, piece.blockY);
        assertEquals(expected.size(), piece.shadowLayers.size());
        for (int k = 0; k < expected.size(); k++) {
          assertTrue(expected.get(k).equals(piece.shadowLayers.get(k)));
        }
        int j = i + 1;
        while (all.get(j).isOverlay()) {
          j++;
        }
        FakePiece next = all.get(j);
        assertTrue(!next.isFace && !next.isShadow, "a shadow must be followed by its tile's top");
        assertEquals(piece.blockX, next.blockX);
        assertEquals(piece.blockY, next.blockY);
      }
    }
    int expected = 0;
    for (FakePiece top : topsByPosition(scene).values()) {
      if (sun.softShadowOn(top.blockX, top.blockY).stream().anyMatch(a -> !a.isEmpty())) {
        expected++;
      }
    }
    assertEquals(expected, shadowCount);
  }

  @Test
  void rimsTraceEveryEdgeWithALowerNeighbour() {
    TerrainScene scene = newScene();
    scene.update(0, 0);
    Map<String, List<Edge>> rims = new HashMap<>();
    for (FakePiece piece : pieces(scene)) {
      if (piece.rimSide != null) {
        rims.computeIfAbsent(piece.blockX + "," + piece.blockY, k -> new ArrayList<>())
            .add(piece.rimSide);
        assertEquals(terrain.blockAt(piece.blockX, piece.blockY), piece.type);
      }
    }
    int total = 0;
    for (FakePiece top : topsByPosition(scene).values()) {
      int x = top.blockX;
      int y = top.blockY;
      int height = terrain.heightAt(x, y);
      List<Edge> expected = new ArrayList<>();
      if (terrain.heightAt(x, y + 1) < height) {
        expected.add(Edge.BACK);
      }
      if (terrain.heightAt(x, y - 1) < height) {
        expected.add(Edge.FRONT);
      }
      if (terrain.heightAt(x - 1, y) < height) {
        expected.add(Edge.LEFT);
      }
      if (terrain.heightAt(x + 1, y) < height) {
        expected.add(Edge.RIGHT);
      }
      assertEquals(expected, rims.getOrDefault(x + "," + y, new ArrayList<>()), "at " + x + "," + y);
      total += expected.size();
    }
    assertTrue(total > 0, "spawn has drop-offs, so some rims are expected");
  }

  @Test
  void occlusionLinesEveryBackLeftAndRightEdgeAgainstHigherGround() {
    TerrainScene scene = newScene();
    scene.update(0, 0);
    Map<String, List<Edge>> occluded = new HashMap<>();
    List<FakePiece> all = pieces(scene);
    for (int i = 0; i < all.size(); i++) {
      FakePiece piece = all.get(i);
      if (piece.occlusionEdge != null) {
        occluded.computeIfAbsent(piece.blockX + "," + piece.blockY, k -> new ArrayList<>())
            .add(piece.occlusionEdge);
        int j = i + 1;
        while (all.get(j).isOverlay()) {
          j++;
        }
        FakePiece top = all.get(j);
        assertTrue(!top.isFace, "occlusion precedes top");
        assertEquals(piece.blockX, top.blockX);
        assertEquals(piece.blockY, top.blockY);
      }
    }
    int total = 0;
    for (FakePiece top : topsByPosition(scene).values()) {
      int x = top.blockX;
      int y = top.blockY;
      int height = terrain.heightAt(x, y);
      List<Edge> expected = new ArrayList<>();
      if (terrain.heightAt(x, y + 1) > height) {
        expected.add(Edge.BACK);
      }
      if (terrain.heightAt(x - 1, y) > height) {
        expected.add(Edge.LEFT);
      }
      if (terrain.heightAt(x + 1, y) > height) {
        expected.add(Edge.RIGHT);
      }
      assertEquals(expected, occluded.getOrDefault(x + "," + y, new ArrayList<>()), x + "," + y);
      total += expected.size();
    }
    assertTrue(total > 0, "spawn has walls, so some occlusion is expected");
  }

  @Test
  void cornerOcclusionAppearsWhereOnlyTheDiagonalBehindIsHigher() {
    TerrainScene scene = newScene();
    scene.update(-46_800, 26_100);
    Map<String, List<Corner>> corners = new HashMap<>();
    for (FakePiece piece : pieces(scene)) {
      if (piece.occlusionCorner != null) {
        corners.computeIfAbsent(piece.blockX + "," + piece.blockY, k -> new ArrayList<>())
            .add(piece.occlusionCorner);
      }
    }
    int total = 0;
    for (FakePiece top : topsByPosition(scene).values()) {
      int x = top.blockX;
      int y = top.blockY;
      int height = terrain.heightAt(x, y);
      boolean backHigher = terrain.heightAt(x, y + 1) > height;
      List<Corner> expected = new ArrayList<>();
      if (terrain.heightAt(x - 1, y + 1) > height
          && !backHigher
          && terrain.heightAt(x - 1, y) <= height) {
        expected.add(Corner.BACK_LEFT);
      }
      if (terrain.heightAt(x + 1, y + 1) > height
          && !backHigher
          && terrain.heightAt(x + 1, y) <= height) {
        expected.add(Corner.BACK_RIGHT);
      }
      assertEquals(expected, corners.getOrDefault(x + "," + y, new ArrayList<>()), x + "," + y);
      total += expected.size();
    }
    assertTrue(total > 0, "hilly ground has outside corners, so some corner occlusion is expected");
  }

  @Test
  void everyTopIsLitBySunlightRightBeforeIt() {
    TerrainScene scene = newScene();
    scene.update(0, 0);
    List<FakePiece> all = pieces(scene);
    int sunlit = 0;
    for (int i = 0; i < all.size(); i++) {
      if (all.get(i).isSunlight) {
        sunlit++;
        FakePiece top = all.get(i + 1);
        assertTrue(!top.isFace && !top.isOverlay(), "sunlight must come right before its top");
        assertEquals(all.get(i).blockX, top.blockX);
        assertEquals(all.get(i).blockY, top.blockY);
      }
    }
    assertEquals(topsByPosition(scene).size(), sunlit);
  }

  @Test
  void everyFaceStepIsDarkenedByAnOverlayRightBeforeIt() {
    TerrainScene scene = newScene();
    scene.update(0, 0);
    List<FakePiece> all = pieces(scene);
    int faces = 0;
    int shades = 0;
    for (int i = 0; i < all.size(); i++) {
      FakePiece piece = all.get(i);
      if (piece.isFaceShade) {
        shades++;
        FakePiece face = all.get(i + 1);
        assertTrue(face.isFace, "a face overlay must be followed by its face");
        assertEquals(piece.blockX, face.blockX);
        assertEquals(piece.blockY, face.blockY);
        assertEquals(piece.value, face.value);
      }
      if (piece.isFace) {
        faces++;
        assertTrue(all.get(i - 1).isFaceShade, "every face must have its overlay");
      }
    }
    assertEquals(faces, shades);
    assertTrue(faces > 0);
  }

  @Test
  void rimsComeBeforeTheirTilesTop() {
    TerrainScene scene = newScene();
    scene.update(0, 0);
    List<FakePiece> all = pieces(scene);
    for (int i = 0; i < all.size(); i++) {
      if (all.get(i).rimSide != null) {
        int j = i + 1;
        while (all.get(j).isOverlay()) {
          j++;
        }
        FakePiece top = all.get(j);
        assertTrue(!top.isFace, "rims and shadows must be followed by their tile's top");
        assertEquals(all.get(i).blockX, top.blockX);
        assertEquals(all.get(i).blockY, top.blockY);
      }
    }
  }

  @Test
  void nearerRowsComeFirstAndEachTopComesBeforeItsFaces() {
    TerrainScene scene = newScene();
    scene.update(0, 0);
    int lastRow = Integer.MIN_VALUE;
    FakePiece previous = null;
    for (FakePiece piece : pieces(scene)) {
      assertTrue(piece.blockY >= lastRow, "rows must go from near (low y) to far");
      lastRow = piece.blockY;
      if (piece.isFace) {
        assertTrue(
            previous != null && previous.blockX == piece.blockX && previous.blockY == piece.blockY,
            "a face must follow its own tile");
      }
      previous = piece;
    }
  }

  @Test
  void movingWithinTheSameBlockChangesNothing() {
    TerrainScene scene = newScene();
    scene.update(10, 10);
    int created = piecesCreated;
    List<FakePiece> before = pieces(scene);
    scene.update(BLOCK - 1, BLOCK - 1);
    assertEquals(created, piecesCreated);
    assertEquals(before, pieces(scene));
  }

  @Test
  void movingOneBlockReusesTilesStillOnScreen() {
    TerrainScene scene = newScene();
    scene.update(0, 0);
    Map<String, FakePiece> before = topsByPosition(scene);
    scene.update(BLOCK, 0);
    Map<String, FakePiece> after = topsByPosition(scene);
    assertSame(before.get("0,0"), after.get("0,0"));
    assertTrue(after.containsKey("9,0"));
    assertTrue(!after.containsKey("-8,0"));
  }

  @Test
  void returningToAnAreaCreatesFreshTiles() {
    TerrainScene scene = newScene();
    scene.update(0, 0);
    FakePiece original = topsByPosition(scene).get("0,0");
    scene.update(100 * BLOCK, 0);
    scene.update(0, 0);
    FakePiece again = topsByPosition(scene).get("0,0");
    assertNotSame(original, again);
    assertEquals(original.type, again.type);
  }
}
