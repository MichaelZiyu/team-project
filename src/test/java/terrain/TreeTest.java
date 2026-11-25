package terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TreeTest {

  @Test
  void trunksStandOnTheGroundAndRiseAsTallAsTheShapeSays() {
    for (Tree.Shape shape : Tree.Shape.values()) {
      long logs =
          shape.blocks().stream().filter(b -> b.material() == Tree.Material.LOG).count();
      assertEquals(shape.trunkBlocks(), logs, shape.name());
      assertTrue(
          shape.blocks().stream()
              .filter(b -> b.material() == Tree.Material.LOG)
              .allMatch(b -> b.dx() == 0 && b.dy() == 0),
          shape.name());
    }
  }

  @Test
  void noTwoBlocksShareTheSamePlace() {
    for (Tree.Shape shape : Tree.Shape.values()) {
      Set<String> places = new HashSet<>();
      for (Tree.Block block : shape.blocks()) {
        assertTrue(
            places.add(block.dx() + "," + block.dy() + "," + block.level()),
            shape + " repeats " + block);
      }
    }
  }

  @Test
  void theCanopyReachesExactlyItsRadius() {
    for (Tree.Shape shape : Tree.Shape.values()) {
      int reach =
          shape.blocks().stream()
              .mapToInt(b -> Math.max(Math.abs(b.dx()), Math.abs(b.dy())))
              .max()
              .orElseThrow();
      assertEquals(shape.radius(), reach, shape.name());
    }
  }

  @Test
  void theBottomOfEveryTrunkShowsBelowItsCanopy() {
    // Seen from the front and above, a block one row nearer hides two levels of the row behind.
    for (Tree.Shape shape : Tree.Shape.values()) {
      int lowestHidden = Integer.MAX_VALUE;
      for (Tree.Block block : shape.blocks()) {
        if (block.material() == Tree.Material.LEAVES && block.dy() < 0) {
          lowestHidden = Math.min(lowestHidden, block.level() + 2 * block.dy());
        }
      }
      assertTrue(lowestHidden > 1, shape + " hides its trunk down to level " + lowestHidden);
    }
  }
}
