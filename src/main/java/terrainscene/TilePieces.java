package terrainscene;

import game.gameModel.Renderable;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import terrain.BlockType;
import terrain.CaveEntrance;
import terrain.Terrain;
import terrain.Trees;

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
  private final WaterPainter water;
  private final boolean waterSurfaces;

  /**
   * Creates a piece builder whose water tiles include their surface.
   *
   * @param terrain the terrain the tiles come from
   * @param factory creates each piece
   */
  TilePieces(Terrain terrain, TerrainScene.TileFactory factory) {
    this(terrain, factory, true);
  }

  /**
   * Creates a piece builder.
   *
   * @param terrain the terrain the tiles come from
   * @param factory creates each piece
   * @param waterSurfaces whether water tiles include their surface; leave it out when the water
   *     is drawn as a layer of its own, underneath (as {@link ChunkPainter} does)
   */
  TilePieces(Terrain terrain, TerrainScene.TileFactory factory, boolean waterSurfaces) {
    this(terrain, factory, waterSurfaces, null);
  }

  /**
   * Creates a piece builder whose shadows include those of trees.
   *
   * @param terrain the terrain the tiles come from
   * @param factory creates each piece
   * @param waterSurfaces whether water tiles include their surface
   * @param trees the trees growing on the terrain, whose shadows fall on it; {@code null} for none
   */
  TilePieces(
      Terrain terrain, TerrainScene.TileFactory factory, boolean waterSurfaces, Trees trees) {
    this.terrain = terrain;
    this.shadows = new SunShadows(terrain, trees);
    this.factory = factory;
    this.water = new WaterPainter(terrain);
    this.waterSurfaces = waterSurfaces;
  }

  /**
   * Returns a tile's rims (one per back, front, left or right edge where the neighbour is
   * lower, as bright as the edge's sunlight), its shadow (if taller terrain toward the light
   * shades it; soft or sharp as the light says), its occlusion (one per back, left or right edge
   * where the neighbour is higher, then one per back corner where only the diagonal neighbour is),
   * its sunlight (if there is any), its glow (if the light brightens tops, and it is not water),
   * its part of a cave entrance's opening if it has one, or else a flower on about one grass tile
   * in {@link Flowers#ONE_IN}, its top (for water, its surface
   * coloured by depth, unless water is drawn separately), then the
   * face steps visible below it: one per level that the tile stands above the tile in front of it,
   * from the top step down, each preceded by the overlay that shades it and, if the light darkens
   * faces, the overlay that darkens it.
   */
  List<Renderable> build(int blockX, int blockY, TerrainLight sun) {
    int height = terrain.heightAt(blockX, blockY);
    BlockType type = terrain.blockAt(blockX, blockY);
    List<Renderable> pieces = new ArrayList<>();
    addRimIfLower(pieces, blockX, blockY, type, height, sun, Edge.BACK, 0, 1);
    addRimIfLower(pieces, blockX, blockY, type, height, sun, Edge.FRONT, 0, -1);
    addRimIfLower(pieces, blockX, blockY, type, height, sun, Edge.LEFT, -1, 0);
    addRimIfLower(pieces, blockX, blockY, type, height, sun, Edge.RIGHT, 1, 0);
    List<Area> shadow =
        sun.softShadows()
            ? shadows.softShadowOn(blockX, blockY, sun)
            : List.of(shadows.shadowOn(blockX, blockY, sun));
    if (shadow.stream().anyMatch(layer -> !layer.isEmpty())) {
      pieces.add(factory.shadow(blockX, blockY, height, shadow, sun.shadowStrength()));
    }
    addOcclusionIfHigher(pieces, blockX, blockY, height, Edge.BACK, 0, 1);
    addOcclusionIfHigher(pieces, blockX, blockY, height, Edge.LEFT, -1, 0);
    addOcclusionIfHigher(pieces, blockX, blockY, height, Edge.RIGHT, 1, 0);
    addCornerOcclusionIfHigher(pieces, blockX, blockY, height, Corner.BACK_LEFT, -1);
    addCornerOcclusionIfHigher(pieces, blockX, blockY, height, Corner.BACK_RIGHT, 1);
    if (sun.sunlight() > 0) {
      pieces.add(factory.sunlight(blockX, blockY, height, sun.sunlight()));
    }
    // Water is not lit up like solid ground; under lightning only its ripples catch the light.
    if (sun.topGlow() > 0 && type != BlockType.WATER) {
      pieces.add(factory.topGlow(blockX, blockY, height, sun.topGlow()));
    }
    CaveEntrance cave = terrain.caveEntranceAt(blockX, blockY);
    if (cave != null) {
      pieces.add(factory.caveEntrance(blockX, blockY, height, cave, type));
    } else if (type == BlockType.GRASS) {
      String flower = Flowers.at(blockX, blockY);
      if (flower != null) {
        pieces.add(factory.flower(blockX, blockY, height, flower));
      }
    }
    if (type != BlockType.WATER) {
      pieces.add(factory.top(blockX, blockY, type, height));
    } else if (waterSurfaces) {
      pieces.add(new WaterRenderable(blockX, blockY, water));
    }
    int frontHeight = terrain.heightAt(blockX, blockY - 1);
    for (int level = height - 1; level >= frontHeight; level--) {
      if (sun.faceDarkening() > 0) {
        pieces.add(factory.faceDarkening(blockX, blockY, level, sun.faceDarkening()));
      }
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
      TerrainLight sun,
      Edge side,
      int stepX,
      int stepY) {
    if (terrain.heightAt(blockX + stepX, blockY + stepY) < height) {
      pieces.add(
          factory.rim(
              blockX, blockY, type, height, side, sun.edgeLight(side), sun.edgeGlow(side)));
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
