package terrain;

/** A region of the world with its own kind of landscape. */
public enum Biome {
  PLAINS,
  DESERT,
  MOUNTAINS,

  /** Underground, inside a cave ({@link CaveTerrain}). */
  CAVE
}
