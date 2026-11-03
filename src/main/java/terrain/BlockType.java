package terrain;

/**
 * A kind of block that terrain can contain.
 *
 * <p>Each block type carries the placeholder label drawn for it until real textures exist.
 */
public enum BlockType {
  GRASS("grass"),
  DIRT("dirt"),
  SAND("sand"),
  STONE("stone"),
  SNOW("snow"),
  WATER("water"),
  AIR("air");

  private final String label;

  BlockType(String label) {
    this.label = label;
  }

  /**
   * Returns the placeholder text drawn for this block type.
   *
   * @return the lowercase name shown on a placeholder tile, such as {@code "grass"}
   */
  public String getLabel() {
    return label;
  }
}
