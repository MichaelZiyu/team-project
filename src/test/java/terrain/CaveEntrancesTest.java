package terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CaveEntrancesTest {

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  private final Terrain terrain = OverworldTerrain.fromSeed(42L);

  private List<CaveEntrance> nearSpawn() {
    return terrain.caveEntrancesNear(0, 0, 300 * BLOCK);
  }

  @Test
  void entrancesAreScatteredAroundTheWorld() {
    List<CaveEntrance> found = nearSpawn();
    // About 113 regions within 300 blocks; many try for an entrance and some find flat land.
    assertTrue(found.size() > 10 && found.size() < 113, "found " + found.size());
  }

  @Test
  void entrancesAreOnFlatDryLandAndTheirTilesFindThem() {
    for (CaveEntrance entrance : nearSpawn()) {
      int height = terrain.heightAt(entrance.minX(), entrance.minY());
      assertTrue(height >= 1, "on land: " + entrance);
      for (int x = entrance.minX() - 1; x <= entrance.minX() + entrance.widthBlocks(); x++) {
        for (int y = entrance.minY() - 1; y <= entrance.minY() + entrance.heightBlocks(); y++) {
          assertEquals(height, terrain.heightAt(x, y), "flat around " + entrance);
          assertNotEquals(BlockType.WATER, terrain.blockAt(x, y));
          if (entrance.covers(x, y)) {
            assertEquals(entrance, terrain.caveEntranceAt(x, y));
          } else {
            assertNull(terrain.caveEntranceAt(x, y), "just outside " + entrance);
          }
        }
      }
    }
  }

  @Test
  void entrancesAreSizedAndShapedWithinLimits() {
    for (CaveEntrance entrance : nearSpawn()) {
      int length = Math.max(entrance.widthBlocks(), entrance.heightBlocks());
      int width = Math.min(entrance.widthBlocks(), entrance.heightBlocks());
      assertTrue(length >= CaveEntrances.MIN_LENGTH && length <= CaveEntrances.MAX_LENGTH);
      assertTrue(width >= CaveEntrances.MIN_WIDTH && width <= CaveEntrances.MAX_WIDTH);
    }
  }

  @Test
  void theSameWorldAlwaysHasTheSameEntrancesAndAnotherWorldOthers() {
    assertEquals(nearSpawn(), OverworldTerrain.fromSeed(42L).caveEntrancesNear(0, 0, 300 * BLOCK));
    assertNotEquals(
        nearSpawn(), OverworldTerrain.fromSeed(7L).caveEntrancesNear(0, 0, 300 * BLOCK));
  }

  @Test
  void nearbyEntrancesComeNearestFirstAndWithinReach() {
    List<CaveEntrance> found = nearSpawn();
    for (int i = 1; i < found.size(); i++) {
      assertTrue(found.get(i - 1).distanceTo(0, 0) <= found.get(i).distanceTo(0, 0));
    }
    CaveEntrance first = found.get(0);
    List<CaveEntrance> close =
        terrain.caveEntrancesNear(first.centreX(), first.centreY(), BLOCK);
    assertEquals(List.of(first), close, "standing on it finds just it");
  }

  @Test
  void anEntranceKnowsItsMiddleAndCoversOnlyItsTiles() {
    CaveEntrance entrance = new CaveEntrance(10, -4, 4, 2, 99);
    assertEquals(12 * BLOCK, entrance.centreX(), 1e-9);
    assertEquals(-3 * BLOCK, entrance.centreY(), 1e-9);
    assertTrue(entrance.covers(10, -4) && entrance.covers(13, -3));
    assertTrue(!entrance.covers(14, -3) && !entrance.covers(10, -2) && !entrance.covers(9, -4));
    assertEquals(5.0 * BLOCK, entrance.distanceTo(12 * BLOCK + 3 * BLOCK, -3 * BLOCK + 4 * BLOCK),
        1e-9);
    assertThrows(IllegalArgumentException.class, () -> new CaveEntrance(0, 0, 0, 2, 1));
  }

  @Test
  void terrainWithoutCavesHasNone() {
    Terrain plain =
        new Terrain() {
          @Override
          public BlockType blockAt(int blockX, int blockY) {
            return BlockType.GRASS;
          }

          @Override
          public int heightAt(int blockX, int blockY) {
            return 1;
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
        };
    assertNull(plain.caveEntranceAt(0, 0));
    assertTrue(plain.caveEntrancesNear(0, 0, 1000).isEmpty());
  }
}
