package terrainscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.gameModel.Renderable;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import terrain.Biome;
import terrain.BlockPos;
import terrain.BlockType;
import terrain.Chunk;
import terrain.Terrain;
import terrain.Tree;
import terrain.Trees;

class TreeSceneTest {

  private static final int VIEW_WIDTH = 800;
  private static final int VIEW_HEIGHT = 600;
  private static final double FRAME = 1.0 / 60;

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

  private final Trees trees = new Trees(PLAINS, 9);
  private final Tree tree = trees.within(-60, -60, 60, 60).get(0);
  private final TreeScene scene =
      new TreeScene(trees, new TreePainter(), VIEW_WIDTH, VIEW_HEIGHT);

  /** The tree's trunk's middle, as drawn: where the player is in front of or behind it. */
  private final int trunkX = tree.blockX() * 64 + 32;
  private final int trunkY = tree.blockY() * 64 + 32;

  private static List<Renderable> list(Iterable<Renderable> trees) {
    List<Renderable> all = new ArrayList<>();
    trees.forEach(all::add);
    return all;
  }

  private TreeScene.TreeRenderable ours(Iterable<Renderable> trees) {
    for (Renderable sprite : trees) {
      if (sprite.getX() == trunkX) {
        return (TreeScene.TreeRenderable) sprite;
      }
    }
    return null;
  }

  @Test
  void playerNearerTheBottomOfTheScreenWalksInFrontOfTheTree() {
    scene.update(trunkX, trunkY - 40, 64, 64, FRAME);
    assertTrue(ours(scene.behind()) != null, "drawn under the player");
    assertEquals(null, ours(scene.inFront()));
  }

  @Test
  void playerFurtherUpTheScreenIsBehindTheTree() {
    scene.update(trunkX, trunkY + 40, 64, 64, FRAME);
    assertTrue(ours(scene.inFront()) != null, "drawn over the player");
    assertEquals(null, ours(scene.behind()));
  }

  @Test
  void treeCoveringThePlayerFadesToSeeThroughThenBack() {
    for (int i = 0; i < 60; i++) {
      scene.update(trunkX, trunkY + 40, 64, 64, FRAME);
    }
    TreeScene.TreeRenderable sprite = ours(scene.inFront());
    assertEquals(TreeScene.SEE_THROUGH_OPACITY, sprite.opacity(), 1e-9);
    BufferedImage faded = TreeSceneTest.imageOf(sprite);
    int mostSolid = 0;
    for (int y = 0; y < faded.getHeight(); y++) {
      for (int x = 0; x < faded.getWidth(); x++) {
        mostSolid = Math.max(mostSolid, faded.getRGB(x, y) >>> 24);
      }
    }
    assertTrue(mostSolid < 200, "see-through everywhere, but alpha reaches " + mostSolid);
    for (int i = 0; i < 60; i++) {
      scene.update(trunkX, trunkY - 40, 64, 64, FRAME);
    }
    assertEquals(1, ours(scene.behind()).opacity(), 1e-9);
  }

  @Test
  void fadingTakesMoreThanOneFrame() {
    scene.update(trunkX, trunkY + 40, 64, 64, FRAME);
    double afterOneFrame = ours(scene.inFront()).opacity();
    assertTrue(afterOneFrame < 1 && afterOneFrame > 0.9, "after a frame: " + afterOneFrame);
  }

  @Test
  void treeInFrontThatDoesNotCoverThePlayerStaysSolid() {
    // Well above the tree and off to the side: behind it in depth, but not hidden by it.
    for (int i = 0; i < 60; i++) {
      scene.update(trunkX + 600, trunkY + 40, 64, 64, FRAME);
    }
    assertEquals(1, ours(scene.inFront()).opacity(), 1e-9);
  }

  @Test
  void nearerTreesComeFirstSoTheyAreDrawnOnTop() {
    scene.update(trunkX, trunkY - 2000, 64, 64, FRAME);
    scene.update(trunkX, trunkY + 2000, 64, 64, FRAME);
    List<Renderable> front = list(scene.inFront());
    for (int i = 1; i < front.size(); i++) {
      TreeScene.TreeRenderable a = (TreeScene.TreeRenderable) front.get(i - 1);
      TreeScene.TreeRenderable b = (TreeScene.TreeRenderable) front.get(i);
      assertTrue(a.depthY() <= b.depthY(), "nearest first");
    }
  }

  @Test
  void eachTreeIsPlacedByItsMiddleAndTheBottomOfItsPicture() {
    scene.update(trunkX, trunkY - 40, 64, 64, FRAME);
    TreeScene.TreeRenderable sprite = ours(scene.behind());
    int[] bounds = TreePainter.bounds(tree.shape());
    assertEquals(tree.blockY() * 64 + bounds[0], sprite.getY());
    assertEquals(bounds[1], sprite.getHeight());
    assertEquals(TreePainter.width(tree.shape()), sprite.getWidth());
  }

  private static BufferedImage imageOf(TreeScene.TreeRenderable sprite) {
    javax.swing.JLabel label = (javax.swing.JLabel) sprite.getTexture().getContent();
    javax.swing.ImageIcon icon = (javax.swing.ImageIcon) label.getIcon();
    return (BufferedImage) icon.getImage();
  }
}
