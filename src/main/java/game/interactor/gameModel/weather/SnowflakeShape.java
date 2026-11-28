package game.interactor.gameModel.weather;

/**
 * The two shapes of snowflake, each drawn as a few square pixels on a 3 by 3 grid, in keeping with
 * the game's pixel-art look.
 */
public enum SnowflakeShape {

  /** A plus: the middle and the four sides. */
  PLUS(new boolean[][] {{false, true, false}, {true, true, true}, {false, true, false}}),

  /** An X: the middle and the four corners. */
  CROSS(new boolean[][] {{true, false, true}, {false, true, false}, {true, false, true}});

  /** How many pixels wide and tall the grid is. */
  public static final int GRID = 3;

  private final boolean[][] filled;

  SnowflakeShape(boolean[][] filled) {
    this.filled = filled;
  }

  /**
   * Returns whether one of the shape's grid squares is filled.
   *
   * @param column from 0 (left) to {@link #GRID} - 1
   * @param row from 0 (top) to {@link #GRID} - 1
   * @return {@code true} if that square is part of the flake
   */
  public boolean isFilled(int column, int row) {
    return filled[row][column];
  }
}
