package terrainscene;

import game.gameModel.Renderable;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import terrain.BlockType;
import terrain.Terrain;

/**
 * Works out every drawable piece of one tile, in drawing order, from the terrain around it.
 *
 * <p>Shared by {@link TerrainScene}, which hands the pieces to the renderer one by one, and {@link
 * ChunkPainter}, which paints them into a chunk image. The list is ordered for the game's renderer,
 * which draws the first piece on top.
 */
final class TilePieces {

  private final Terrain terrain;
  private final SunShadows shadows;
  private final TerrainScene.TileFactory factory;

  /**
   * Creates a piece builder.
   *
   * @param terrain the terrain the tiles come from
   * @param factory creates each piece
   */
  TilePieces(Terrain terrain, TerrainScene.TileFactory factory) {
    this.terrain = terrain;
    this.shadows = new SunShadows(terrain);
    this.factory = factory;
  }

  /**
   * Returns a tile's rims (one per back, front, left or right edge where the neighbour is
   * lower, as bright as the edge's sunlight), its shadow (if taller terrain toward the sun shades
   * it), its occlusion (one per back, left or right edge where the neighbour is higher, then one
   * per back corner where only the diagonal neighbour is), its sunlight (during the day), its top,
   * then the face steps visible below it: one per level that the tile stands above the tile in
   * front of it, from the top step down, each preceded by the overlay that shades it.
   */
  List<Renderable> build(int blockX, int blockY, ShadowCast sun) {
    int height = terrain.heightAt(blockX, blockY);
    BlockType type = terrain.blockAt(blockX, blockY);
    List<Renderable> pieces = new ArrayList<>();
    addRimIfLower(pieces, blockX, blockY, type, height, sun, Edge.BACK, 0, 1);
    addRimIfLower(pieces, blockX, blockY, type, height, sun, Edge.FRONT, 0, -1);
    addRimIfLower(pieces, blockX, blockY, type, height, sun, Edge.LEFT, -1, 0);
    addRimIfLower(pieces, blockX, blockY, type, height, sun, Edge.RIGHT, 1, 0);
    List<Area> shadow = shadows.softShadowOn(blockX, blockY, sun);
    if (shadow.stream().anyMatch(layer -> !layer.isEmpty())) {
      pieces.add(factory.shadow(blockX, blockY, height, shadow, sun.strength()));
    }
    addOcclusionIfHigher(pieces, blockX, blockY, height, Edge.BACK, 0, 1);
    addOcclusionIfHigher(pieces, blockX, blockY, height, Edge.LEFT, -1, 0);
    addOcclusionIfHigher(pieces, blockX, blockY, height, Edge.RIGHT, 1, 0);
    addCornerOcclusionIfHigher(pieces, blockX, blockY, height, Corner.BACK_LEFT, -1);
    addCornerOcclusionIfHigher(pieces, blockX, blockY, height, Corner.BACK_RIGHT, 1);
    if (sun.isDaylight()) {
      pieces.add(factory.sunlight(blockX, blockY, height, sun.strength()));
    }
    pieces.add(factory.top(blockX, blockY, type, height));
    int frontHeight = terrain.heightAt(blockX, blockY - 1);
    for (int level = height - 1; level >= frontHeight; level--) {
      pieces.add(factory.faceShade(blockX, blockY, level));
      pieces.add(factory.face(blockX, blockY, type, level, level == height - 1));
    }
    return Collections.unmodifiableList(pieces);
  }

  /** Adds a rim on one side if the neighbouring tile in that direction is lower. */
  private void addRimIfLower(
      List<Renderable> pieces,
      int blockX,
      int blockY,
      BlockType type,
      int height,
      ShadowCast sun,
      Edge side,
      int stepX,
      int stepY) {
    if (terrain.heightAt(blockX + stepX, blockY + stepY) < height) {
      pieces.add(factory.rim(blockX, blockY, type, height, side, sun.edgeLight(side)));
    }
  }

  /** Adds an occlusion gradient on one edge if the neighbouring tile that way is higher. */
  private void addOcclusionIfHigher(
      List<Renderable> pieces,
      int blockX,
      int blockY,
      int height,
      Edge edge,
      int stepX,
      int stepY) {
    if (terrain.heightAt(blockX + stepX, blockY + stepY) > height) {
      pieces.add(factory.occlusion(blockX, blockY, height, edge));
    }
  }

  /**
   * Adds occlusion in a back corner if the diagonal neighbour behind it is higher but neither
   * tile beside that corner is, since their edge strips would already darken it.
   */
  private void addCornerOcclusionIfHigher(
      List<Renderable> pieces, int blockX, int blockY, int height, Corner corner, int stepX) {
    boolean diagonalHigher = terrain.heightAt(blockX + stepX, blockY + 1) > height;
    boolean backHigher = terrain.heightAt(blockX, blockY + 1) > height;
    boolean sideHigher = terrain.heightAt(blockX + stepX, blockY) > height;
    if (diagonalHigher && !backHigher && !sideHigher) {
      pieces.add(factory.cornerOcclusion(blockX, blockY, height, corner));
    }
  }

}
