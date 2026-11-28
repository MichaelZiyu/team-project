package game.interactor.gameModel.weather;

import java.util.Random;

/**
 * Where falling weather (raindrops, snowflakes) can fall around the player, and where it lands.
 *
 * <p>Everything falls onto ground spread evenly over the area that can appear on screen;
 * perspective then spreads it evenly over the screen too, wherever the vanishing point is. When
 * the vanishing point is off the screen, that area also takes in the ground between the screen
 * and the vanishing point, so more is simulated to keep the same amount in view. Each piece starts
 * just high enough to appear beyond the edge of the screen, so weather streams in from the edges
 * rather than popping up in the middle.
 *
 * <p>Given the ground ({@link #setGround}), things land on the tops of raised tiles, lifted up
 * the screen as the game draws them; a spot hidden behind a taller tile in front can be found with
 * {@link #hiddenInFront}. Until then the ground is flat.
 */
final class FallingArea {

  /**
   * The highest anything can be, in pixels: almost at the camera. Things start lower than this,
   * just high enough to be out of view; only those falling almost under the vanishing point need
   * it.
   */
  static final double MAX_HEIGHT_PX = 0.99 * RainPerspective.CAMERA_HEIGHT_PX;

  /** How far beyond the edge of the screen things start, as a share of their distance out. */
  static final double START_BEYOND_EDGE = 0.03;

  /** Extra space around the screen where things may fall, so the edges never look empty. */
  static final double MARGIN_PX = 96;

  /** How many rows in front of a spot to check for a taller tile hiding it: 16 levels of lift. */
  static final int ROWS_THAT_CAN_HIDE = 8;

  /** How many times to pick a landing spot before accepting one that cannot start out of view. */
  private static final int SPOT_TRIES = 20;

  private static final int BLOCK = Ground.BLOCK_PX;
  private static final int STEP = Ground.STEP_PX;

  private final RainPerspective perspective;
  private final int viewWidthPx;
  private final int viewHeightPx;
  private Ground ground = Ground.FLAT;

  /**
   * Creates the area for a screen.
   *
   * @param perspective how heights appear on screen
   * @param viewWidthPx the screen width, in pixels
   * @param viewHeightPx the screen height, in pixels
   */
  FallingArea(RainPerspective perspective, int viewWidthPx, int viewHeightPx) {
    this.perspective = perspective;
    this.viewWidthPx = viewWidthPx;
    this.viewHeightPx = viewHeightPx;
  }

  /**
   * Gives the ground to land on.
   *
   * @param ground the ground, or {@code null} for flat ground
   */
  void setGround(Ground ground) {
    this.ground = ground == null ? Ground.FLAT : ground;
  }

  /**
   * Returns how many pieces to simulate for a given number per screen, scaled up by how much
   * bigger the area is than the screen, so the same amount is in view.
   *
   * @param perScreen how many over the screen and its margin
   * @return how many over the whole area
   */
  int scaledCount(int perScreen) {
    double[] area = area();
    double areaSize = (area[2] - area[0]) * (area[3] - area[1]);
    double screenSize = (viewWidthPx + 2 * MARGIN_PX) * (viewHeightPx + 2 * MARGIN_PX);
    return (int) Math.round(perScreen * areaSize / screenSize);
  }

  /**
   * Returns the ground, in screen coordinates, that anything up to {@link #MAX_HEIGHT_PX} high can
   * appear in front of the screen from, as {@code {left, top, right, bottom}}: the screen (with a
   * margin) together with the screen shrunk toward the vanishing point by the perspective at the
   * greatest height.
   */
  double[] area() {
    double left = -MARGIN_PX;
    double top = -MARGIN_PX;
    double right = viewWidthPx + MARGIN_PX;
    double bottom = viewHeightPx + MARGIN_PX;
    double[] shrunkTopLeft = perspective.unproject(left, top, MAX_HEIGHT_PX);
    double[] shrunkBottomRight = perspective.unproject(right, bottom, MAX_HEIGHT_PX);
    return new double[] {
      Math.min(left, shrunkTopLeft[0]),
      Math.min(top, shrunkTopLeft[1]),
      Math.max(right, shrunkBottomRight[0]),
      Math.max(bottom, shrunkBottomRight[1])
    };
  }

  /**
   * Picks a random spot of the area, and a height just high enough to be out of view above it.
   * Spots so close under the vanishing point that anything falling there would be in view even
   * almost at the camera are picked again: it would pop up in the middle of the screen.
   *
   * @param random where the randomness comes from
   * @param cameraX the camera's x position, in pixels
   * @param cameraY the camera's y position, in pixels
   * @return {@code {groundX, groundY, height}}: the spot in world pixels, and the height in pixels
   */
  double[] pickStart(Random random, double cameraX, double cameraY) {
    double[] area = area();
    double screenX = 0;
    double screenY = 0;
    double height = MAX_HEIGHT_PX;
    for (int tries = 0; tries < SPOT_TRIES; tries++) {
      screenX = area[0] + random.nextDouble() * (area[2] - area[0]);
      screenY = area[1] + random.nextDouble() * (area[3] - area[1]);
      height = startHeight(screenX, screenY);
      if (height < MAX_HEIGHT_PX || !onScreen(screenX, screenY, height)) {
        break;
      }
    }
    // Screen to world: the reverse of RainPerspective.groundToScreen.
    return new double[] {
      screenX - viewWidthPx / 2.0 + cameraX, viewHeightPx / 2.0 - screenY + cameraY, height
    };
  }

  /**
   * Returns how high something falling onto the given ground must start to appear just beyond the
   * edge of the screen: the lowest height at which it is out of view across or up and down.
   * Things falling almost under the vanishing point cannot get out of view; they start at {@link
   * #MAX_HEIGHT_PX}.
   *
   * @param groundScreenX where the ground appears on screen, x, in pixels
   * @param groundScreenY where the ground appears on screen, y, in pixels
   * @return the starting height, in pixels
   */
  double startHeight(double groundScreenX, double groundScreenY) {
    double fromCentreX = groundScreenX - perspective.vanishingX();
    double fromCentreY = groundScreenY - perspective.vanishingY();
    double edgeX = fromCentreX > 0 ? viewWidthPx + MARGIN_PX : -MARGIN_PX;
    double edgeY = fromCentreY > 0 ? viewHeightPx + MARGIN_PX : -MARGIN_PX;
    double height = MAX_HEIGHT_PX;
    if (fromCentreX != 0) {
      double spread = (edgeX - perspective.vanishingX()) / fromCentreX;
      if (spread > 0) {
        height = Math.min(height, perspective.heightForSpreadX(spread * (1 + START_BEYOND_EDGE)));
      }
    }
    if (fromCentreY != 0) {
      double spread = (edgeY - perspective.vanishingY()) / fromCentreY;
      if (spread > 0) {
        height = Math.min(height, perspective.heightForSpreadY(spread * (1 + START_BEYOND_EDGE)));
      }
    }
    return height;
  }

  /**
   * Returns whether something is still falling onto the area around the camera.
   *
   * @param groundX its spot's x, in world pixels
   * @param groundY its spot's y, in world pixels
   * @param cameraX the camera's x position, in pixels
   * @param cameraY the camera's y position, in pixels
   * @return {@code false} once the player has walked away from it, or the wind blown it away
   */
  boolean covers(double groundX, double groundY, double cameraX, double cameraY) {
    double[] area = area();
    double[] ground = perspective.groundToScreen(groundX, groundY, cameraX, cameraY);
    return ground[0] >= area[0] - MARGIN_PX
        && ground[0] <= area[2] + MARGIN_PX
        && ground[1] >= area[1] - MARGIN_PX
        && ground[1] <= area[3] + MARGIN_PX;
  }

  /**
   * Returns a spot moved across the area if it has left it to the east or west, by the area's
   * width, so that it comes back in from the other side. Something blown off one side by the wind
   * reappears on the other at the same height, keeping its fall evenly spread; it stays out of view
   * while it does, since anything over ground beyond the screen's sides appears beyond them too.
   *
   * @param groundX the spot's x, in world pixels
   * @param cameraX the camera's x position, in pixels
   * @return the spot's x, wrapped back into the area if it had left it sideways
   */
  double wrapAcross(double groundX, double cameraX) {
    double[] area = area();
    double screenX = groundX - cameraX + viewWidthPx / 2.0;
    double width = area[2] - area[0] + 2 * MARGIN_PX;
    if (screenX > area[2] + MARGIN_PX) {
      return groundX - width;
    }
    if (screenX < area[0] - MARGIN_PX) {
      return groundX + width;
    }
    return groundX;
  }

  /**
   * Returns how far up the screen the tile top at a world position is drawn: the height something
   * falling there lands at.
   *
   * @param worldX the x position, in world pixels
   * @param worldY the y position, in world pixels
   * @return the lift, in pixels; 0 on flat ground
   */
  int liftAt(double worldX, double worldY) {
    return ground.heightAt(Ground.tileAt(worldX), Ground.tileAt(worldY)) * STEP;
  }

  /**
   * Returns whether a spot on a tile top is hidden behind a taller tile in front of it (further
   * south). A tile in front is drawn lifted by its height; it covers the spot if its far edge is
   * drawn above the spot.
   *
   * @param worldX the spot's x, in world pixels
   * @param worldY the spot's y, in world pixels
   * @param liftPx how far up the screen the spot's tile top is drawn
   * @return {@code true} if something in front hides it
   */
  boolean hiddenInFront(double worldX, double worldY, int liftPx) {
    int blockX = Ground.tileAt(worldX);
    int blockY = Ground.tileAt(worldY);
    // The spot's height on screen, measured like world y: higher is further up the screen.
    double spotY = worldY + liftPx;
    for (int rowsInFront = 1; rowsInFront <= ROWS_THAT_CAN_HIDE; rowsInFront++) {
      int frontY = blockY - rowsInFront;
      double farEdge = (frontY + 1) * (double) BLOCK + ground.heightAt(blockX, frontY) * STEP;
      if (spotY < farEdge) {
        return true;
      }
    }
    return false;
  }

  private boolean onScreen(double groundScreenX, double groundScreenY, double height) {
    double[] at = perspective.project(groundScreenX, groundScreenY, height);
    return at[0] >= 0 && at[0] <= viewWidthPx && at[1] >= 0 && at[1] <= viewHeightPx;
  }
}
