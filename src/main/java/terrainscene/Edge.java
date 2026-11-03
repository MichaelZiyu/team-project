package terrainscene;

/** One edge of a tile's top, as seen from the top-down camera. */
public enum Edge {
  /** The edge furthest from the camera (the top of the tile on screen). */
  BACK,
  /** The edge nearest the camera (the bottom of the tile on screen), above any face. */
  FRONT,
  /** The left edge. */
  LEFT,
  /** The right edge. */
  RIGHT
}
