package terrainscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import terrain.Biome;
import terrain.BlockPos;
import terrain.BlockType;
import terrain.Chunk;
import terrain.OverworldTerrain;
import terrain.Terrain;

class SunShadowsTest {

  /** Flat terrain at height 1 everywhere, except for heights set by the test. */
  private static final class FakeTerrain implements Terrain {
    private final Map<String, Integer> heights = new HashMap<>();

    FakeTerrain set(int blockX, int blockY, int height) {
      heights.put(blockX + "," + blockY, height);
      return this;
    }

    @Override
    public int heightAt(int blockX, int blockY) {
      return heights.getOrDefault(blockX + "," + blockY, 1);
    }

    @Override
    public BlockType blockAt(int blockX, int blockY) {
      return BlockType.GRASS;
    }

    @Override
    public Biome biomeAt(int blockX, int blockY) {
      return Biome.PLAINS;
    }

    @Override
    public Chunk chunkAt(int chunkX, int chunkY) {
      throw new UnsupportedOperationException();
    }

    @Override
    public BlockPos spawnPoint() {
      return new BlockPos(0, 0);
    }
  }

  @Test
  void flatGroundHasNoShadows() {
    assertTrue(new SunShadows(new FakeTerrain()).shadowOn(0, 0).isEmpty());
  }

  @Test
  void oneLevelStepToTheRightShadesARightBandThatSlantsSouth() {
    // One level up: the shadow reaches 32 px left and 16 px south.
    Area shadow = new SunShadows(new FakeTerrain().set(1, 0, 2)).shadowOn(0, 0);
    assertTrue(shadow.contains(60, 60), "right edge, near the top, is shaded");
    assertTrue(shadow.contains(40, 20), "inside the band, lower down, is shaded");
    assertFalse(shadow.contains(40, 60), "the band's top-left is cut by the slant");
    assertFalse(shadow.contains(20, 30), "left of the band is lit");
  }

  @Test
  void stepShadowSpillsSouthOntoTheTileInFrontOfItsNeighbour() {
    // The step at (1, 0) also shades the top of the tile south of (0, 0).
    Area shadow = new SunShadows(new FakeTerrain().set(1, 0, 2)).shadowOn(0, -1);
    assertTrue(shadow.contains(60, 56), "a thin band at the top right is shaded");
    assertFalse(shadow.contains(60, 40), "below that band is lit");
  }

  @Test
  void higherGroundToTheNorthShadesTheTileInFront() {
    // Two levels up to the north: 64 px left and 32 px south.
    Area shadow = new SunShadows(new FakeTerrain().set(0, 1, 3)).shadowOn(0, 0);
    assertTrue(shadow.contains(10, 50), "the tile's top-left is shaded");
    assertFalse(shadow.contains(60, 60), "the top-right corner is lit");
    assertFalse(shadow.contains(30, 10), "the bottom is lit");
  }

  @Test
  void lightComesFromTheRightAndNorthSoTheOtherSidesAreLit() {
    FakeTerrain terrain = new FakeTerrain().set(0, 0, 4);
    SunShadows shadows = new SunShadows(terrain);
    assertTrue(shadows.shadowOn(1, 0).isEmpty(), "east of the tall tile");
    assertTrue(shadows.shadowOn(0, 1).isEmpty(), "north of the tall tile");
    assertFalse(shadows.shadowOn(-1, 0).isEmpty(), "west of the tall tile");
  }

  @Test
  void tilesAsHighAsTheCasterAreNotShaded() {
    SunShadows shadows = new SunShadows(new FakeTerrain().set(1, 0, 3).set(0, 0, 3));
    assertTrue(shadows.shadowOn(0, 0).isEmpty());
  }

  @Test
  void tallCliffCoversTheWholeNeighbourAndReachesFurther() {
    SunShadows shadows = new SunShadows(new FakeTerrain().set(2, 0, 4));
    Area next = shadows.shadowOn(1, 0);
    assertTrue(next.contains(1, 10) && next.contains(63, 30), "the tile next door is shaded");
    assertFalse(next.contains(1, 60), "its top-left is lit, because the shadow slants south");
    assertFalse(shadows.shadowOn(0, 0).isEmpty(), "a 3-level shadow reaches two tiles over");
  }

  @Test
  void softShadowStartsWithTheMainShadowAndFadesFurtherFromTheWall() {
    SunShadows shadows = new SunShadows(new FakeTerrain().set(2, 0, 4));
    List<Area> layers = shadows.softShadowOn(1, 0);
    assertEquals(9, layers.size());
    assertTrue(layers.get(0).equals(shadows.shadowOn(1, 0)));
    // Near the wall every sun position agrees; at the far tip only some of them reach.
    int nearWall = 0;
    int farTip = 0;
    for (Area layer : layers) {
      nearWall += layer.contains(60, 30) ? 1 : 0;
      farTip += layer.contains(2, 30) ? 1 : 0;
    }
    assertEquals(9, nearWall);
    assertTrue(farTip > 0 && farTip < 9, "the far edge is soft: " + farTip + " of 9");
  }

  @Test
  void flatGroundHasNoSoftShadowEither() {
    SunShadows shadows = new SunShadows(new FakeTerrain());
    for (Area layer : shadows.softShadowOn(0, 0)) {
      assertTrue(layer.isEmpty());
    }
  }

  @Test
  void realTerrainShadowsStayInsideTheirTile() {
    SunShadows shadows = new SunShadows(OverworldTerrain.fromSeed(42L));
    Rectangle2D tile = new Rectangle2D.Double(0, 0, 64, 64);
    int shaded = 0;
    for (int blockX = -200; blockX < 200; blockX += 3) {
      for (int blockY = -200; blockY < 200; blockY += 7) {
        Area shadow = shadows.shadowOn(blockX, blockY);
        if (!shadow.isEmpty()) {
          shaded++;
          Rectangle2D bounds = shadow.getBounds2D();
          assertTrue(tile.contains(bounds) || tile.equals(bounds), "outside tile: " + bounds);
        }
      }
    }
    assertTrue(shaded > 0, "hilly terrain should cast some shadows");
  }
}
