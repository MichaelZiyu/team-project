package terrainscene;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import terrain.Biome;
import terrain.BlockPos;
import terrain.BlockType;
import terrain.Chunk;
import terrain.Terrain;
import terrain.Tree;
import terrain.Trees;

class TreeShadowsTest {

  /** Flat plains grass everywhere, at height 0. */
  private static final Terrain PLAINS =
      new Terrain() {
        @Override
        public BlockType blockAt(int blockX, int blockY) {
          return BlockType.GRASS;
        }

        @Override
        public int heightAt(int blockX, int blockY) {
          return 0;
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

  /** An evening sun, low in the west: shadows fall east and a little south. */
  private static final TerrainLight EVENING = new TerrainLight(48, -16, 1, 1, true, 0, 0);

  private final Trees trees = new Trees(PLAINS, 9);
  private final Tree tree = trees.within(-60, -60, 60, 60).get(0);
  private final SunShadows withTrees = new SunShadows(PLAINS, trees);

  /** Returns whether any tile in a rectangle around the tree, relative to its trunk, is shaded. */
  private boolean anyShade(
      SunShadows shadows, int fromEast, int toEast, int fromNorth, int toNorth, TerrainLight sun) {
    for (int east = fromEast; east <= toEast; east++) {
      for (int north = fromNorth; north <= toNorth; north++) {
        if (!shadows.shadowOn(tree.blockX() + east, tree.blockY() + north, sun).isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }

  @Test
  void treesShadeTheGroundAwayFromTheSun() {
    // The default sun is to the east and a little north, so shadows fall west and south.
    assertTrue(anyShade(withTrees, -6, -1, -4, 0, TerrainLight.DEFAULT));
    assertFalse(anyShade(new SunShadows(PLAINS), -6, -1, -4, 0, TerrainLight.DEFAULT));
  }

  @Test
  void groundTowardTheSunStaysLit() {
    assertFalse(anyShade(withTrees, 3, 8, -2, 3, TerrainLight.DEFAULT));
  }

  @Test
  void groundUnderTheCanopysSunwardEdgeIsLitBecauseTheCanopyFloats() {
    // Just east of the trunk, under the canopy: the canopy's shadow starts well away from it.
    assertTrue(
        withTrees.shadowOn(tree.blockX() + 1, tree.blockY(), TerrainLight.DEFAULT).isEmpty());
  }

  @Test
  void shadowsSwingToTheOtherSideInTheEvening() {
    assertTrue(anyShade(withTrees, 1, 8, -4, 0, EVENING));
    assertFalse(anyShade(withTrees, -8, -2, -1, 2, EVENING));
  }

  @Test
  void nothingIsShadedAtNight() {
    assertFalse(anyShade(withTrees, -8, 8, -8, 8, TerrainLight.NIGHT));
  }

  @Test
  void softShadowsIncludeTrees() {
    boolean shaded = false;
    for (int east = -6; east <= -1 && !shaded; east++) {
      for (int north = -4; north <= 0 && !shaded; north++) {
        shaded =
            withTrees.softShadowOn(tree.blockX() + east, tree.blockY() + north).stream()
                .anyMatch(area -> !area.isEmpty());
      }
    }
    assertTrue(shaded);
  }
}
