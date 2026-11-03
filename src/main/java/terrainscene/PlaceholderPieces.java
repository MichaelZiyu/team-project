package terrainscene;

import game.gameModel.Renderable;
import java.awt.geom.Area;
import terrain.BlockType;

/** Creates the standard terrain pieces, showing the images in {@code textures/topdown/terrain}. */
final class PlaceholderPieces implements TerrainScene.TileFactory {

  @Override
  public Renderable top(int blockX, int blockY, BlockType type, int height) {
    return new TileRenderable(blockX, blockY, type, height);
  }

  @Override
  public Renderable face(int blockX, int blockY, BlockType tileType, int level, boolean topStep) {
    return new FaceRenderable(blockX, blockY, tileType, level, topStep);
  }

  @Override
  public Renderable shadow(int blockX, int blockY, int height, Area shape) {
    return new ShadowRenderable(blockX, blockY, height, shape);
  }

  @Override
  public Renderable rim(int blockX, int blockY, BlockType type, int height, Edge side) {
    return new RimRenderable(blockX, blockY, type, height, side);
  }

  @Override
  public Renderable occlusion(int blockX, int blockY, int height, Edge edge) {
    return new OcclusionRenderable(blockX, blockY, height, edge);
  }

  @Override
  public Renderable faceShade(int blockX, int blockY, int level) {
    return new FaceShadeRenderable(blockX, blockY, level);
  }
}
