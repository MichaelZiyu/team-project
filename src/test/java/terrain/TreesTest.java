package terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TreesTest {

  private static final int AREA = 200;

  private static List<Tree> treesOn(Terrain terrain, long seed) {
    return new Trees(terrain, seed).within(-AREA, -AREA, AREA, AREA);
  }

  @Test
  void treesGrowOnFlatPlainsGrass() {
    List<Tree> trees = treesOn(new FlatTerrain(BlockType.GRASS, Biome.PLAINS, 2), 1);
    assertTrue(trees.size() > 100, "trees: " + trees.size());
    assertTrue(trees.stream().allMatch(tree -> tree.groundHeight() == 2));
  }

  @Test
  void noTreesGrowOutsideThePlains() {
    assertTrue(treesOn(new FlatTerrain(BlockType.SAND, Biome.DESERT, 0), 1).isEmpty());
    assertTrue(treesOn(new FlatTerrain(BlockType.SNOW, Biome.MOUNTAINS, 5), 1).isEmpty());
  }

  @Test
  void noTreesGrowOnWaterOrBareDirt() {
    assertTrue(treesOn(new FlatTerrain(BlockType.WATER, Biome.PLAINS, 0), 1).isEmpty());
    assertTrue(treesOn(new FlatTerrain(BlockType.DIRT, Biome.PLAINS, 0), 1).isEmpty());
  }

  @Test
  void canopiesNeverOverlap() {
    List<Tree> trees = treesOn(new FlatTerrain(BlockType.GRASS, Biome.PLAINS, 0), 2);
    for (int i = 0; i < trees.size(); i++) {
      for (int j = i + 1; j < trees.size(); j++) {
        Tree a = trees.get(i);
        Tree b = trees.get(j);
        int apart = Math.max(Math.abs(a.blockX() - b.blockX()), Math.abs(a.blockY() - b.blockY()));
        assertTrue(apart > a.shape().radius() + b.shape().radius(), a + " and " + b);
      }
    }
  }

  @Test
  void theSameSeedGrowsTheSameTrees() {
    Terrain terrain = new FlatTerrain(BlockType.GRASS, Biome.PLAINS, 0);
    assertEquals(treesOn(terrain, 3), treesOn(terrain, 3));
    assertFalse(treesOn(terrain, 3).equals(treesOn(terrain, 4)));
  }

  @Test
  void treeIsFoundByItsTrunksTile() {
    Trees trees = new Trees(new FlatTerrain(BlockType.GRASS, Biome.PLAINS, 0), 5);
    Tree tree = trees.within(-AREA, -AREA, AREA, AREA).get(0);
    assertEquals(Optional.of(tree), trees.at(tree.blockX(), tree.blockY()));
    assertEquals(Optional.empty(), trees.at(tree.blockX() + 1, tree.blockY()));
  }

  @Test
  void treesClusterIntoGrovesAndOpenFields() {
    Trees trees = new Trees(new FlatTerrain(BlockType.GRASS, Biome.PLAINS, 0), 6);
    double least = 1;
    double most = 0;
    for (int x = -AREA; x <= AREA; x += 10) {
      for (int y = -AREA; y <= AREA; y += 10) {
        least = Math.min(least, trees.chanceAt(x, y));
        most = Math.max(most, trees.chanceAt(x, y));
      }
    }
    assertTrue(least < 0.2 && most > 0.7, "chance from " + least + " to " + most);
  }

  @Test
  void realWorldTreesStandOnPlainsGrass() {
    // The real world has trees somewhere near its spawn, all on plains grass.
    Terrain world = OverworldTerrain.fromSeed(42L);
    List<Tree> trees = new Trees(world, 42L).within(-100, -100, 100, 100);
    assertFalse(trees.isEmpty());
    for (Tree tree : trees) {
      assertEquals(Biome.PLAINS, world.biomeAt(tree.blockX(), tree.blockY()));
      assertEquals(BlockType.GRASS, world.blockAt(tree.blockX(), tree.blockY()));
    }
  }
}
