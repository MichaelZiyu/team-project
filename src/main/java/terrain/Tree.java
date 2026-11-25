package terrain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A tree standing on the terrain, built of blocks the same size as the terrain's: a trunk of log
 * blocks with a canopy of leaf blocks around and above its top.
 *
 * @param blockX the x coordinate of the tile its trunk stands on, in blocks
 * @param blockY the y coordinate of the tile its trunk stands on, in blocks
 * @param groundHeight the height of the ground it stands on, in levels
 * @param shape its size and shape
 */
public record Tree(int blockX, int blockY, int groundHeight, Tree.Shape shape) {

  /** What a block of a tree is made of. */
  public enum Material {
    /** Part of the trunk. */
    LOG,
    /** Part of the canopy. */
    LEAVES
  }

  /**
   * One block of a tree, placed relative to the tile its trunk stands on.
   *
   * @param dx how many blocks east of the trunk
   * @param dy how many blocks north of the trunk
   * @param level how many blocks above the ground: 1 is the block resting on the ground
   * @param material what it is made of
   */
  public record Block(int dx, int dy, int level, Material material) {}

  /** The kinds of tree. */
  public enum Shape {
    /** A trunk four blocks tall under a canopy three blocks across. */
    SMALL(4, 1),
    /** A trunk five blocks tall under a canopy three blocks across. */
    TALL(5, 1),
    /** A trunk six blocks tall under a wide canopy five blocks across. */
    BROAD(6, 2);

    private final int trunkBlocks;
    private final int radius;
    private final List<Block> blocks;

    Shape(int trunkBlocks, int radius) {
      this.trunkBlocks = trunkBlocks;
      this.radius = radius;
      this.blocks = build(trunkBlocks, radius);
    }

    /**
     * Returns how many blocks tall the trunk is.
     *
     * @return the trunk's height, in blocks
     */
    public int trunkBlocks() {
      return trunkBlocks;
    }

    /**
     * Returns how far the canopy reaches from the trunk.
     *
     * @return the most blocks any block is east, west, north or south of the trunk
     */
    public int radius() {
      return radius;
    }

    /**
     * Returns every block of a tree of this shape.
     *
     * @return the blocks, in no particular order
     */
    public List<Block> blocks() {
      return blocks;
    }

    /**
     * Returns how many blocks tall a tree of this shape is, from the ground to its top.
     *
     * @return the highest level any block reaches
     */
    public int topLevel() {
      return blocks.stream().mapToInt(Block::level).max().orElse(0);
    }

    /**
     * Builds the blocks: the trunk; then, from its top block, a ring of leaves around it as wide
     * as the canopy, a full layer of that width above, a three-wide layer above that if the canopy
     * is wider, and a plus of leaves on top. Five-wide layers leave their far corners off.
     *
     * <p>Seen from the front and above, each row of blocks nearer the viewer hides two levels of
     * the row behind it, so the canopy's front row hides the trunk up to two levels below the
     * canopy for each block the canopy reaches out. The trunks are tall enough that the bottom
     * of every tree's trunk still shows below its canopy.
     */
    private static List<Block> build(int trunk, int radius) {
      List<Block> blocks = new ArrayList<>();
      for (int level = 1; level <= trunk; level++) {
        blocks.add(new Block(0, 0, level, Material.LOG));
      }
      addLayer(blocks, trunk, radius, true);
      addLayer(blocks, trunk + 1, radius, false);
      int top = trunk + 2;
      if (radius == 2) {
        addLayer(blocks, top, 1, false);
        top++;
      }
      blocks.add(new Block(0, 0, top, Material.LEAVES));
      blocks.add(new Block(1, 0, top, Material.LEAVES));
      blocks.add(new Block(-1, 0, top, Material.LEAVES));
      blocks.add(new Block(0, 1, top, Material.LEAVES));
      blocks.add(new Block(0, -1, top, Material.LEAVES));
      return Collections.unmodifiableList(blocks);
    }

    /** Adds a square layer of leaves, leaving out the far corners of a wide one. */
    private static void addLayer(List<Block> blocks, int level, int radius, boolean aroundTrunk) {
      for (int dx = -radius; dx <= radius; dx++) {
        for (int dy = -radius; dy <= radius; dy++) {
          boolean trunk = dx == 0 && dy == 0;
          boolean farCorner = radius == 2 && Math.abs(dx) == 2 && Math.abs(dy) == 2;
          if ((trunk && aroundTrunk) || farCorner) {
            continue;
          }
          blocks.add(new Block(dx, dy, level, Material.LEAVES));
        }
      }
    }
  }
}
