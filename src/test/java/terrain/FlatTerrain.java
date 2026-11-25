package terrain;

/** A test terrain: the same block, biome and height everywhere. */
final class FlatTerrain implements Terrain {

  private final BlockType block;
  private final Biome biome;
  private final int height;

  FlatTerrain(BlockType block, Biome biome, int height) {
    this.block = block;
    this.biome = biome;
    this.height = height;
  }

  @Override
  public BlockType blockAt(int blockX, int blockY) {
    return block;
  }

  @Override
  public int heightAt(int blockX, int blockY) {
    return height;
  }

  @Override
  public Biome biomeAt(int blockX, int blockY) {
    return biome;
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
