package terrainscene;

import cave.CaveMover;
import game.gameModel.Renderable;
import java.util.Collections;
import java.util.Iterator;
import jswing.Texture;

/**
 * The player in a side-view cave: walking left or right as they move, facing the viewer while
 * standing still, and facing the wall while climbing it. Uses the player's own sprites.
 *
 * <p>Call {@link #update} each frame; the sprite's walking frames only move on while its limbs
 * do, and its standing frames (blinking) always do.
 */
public final class CavePlayerSprite implements Iterable<Renderable>, Renderable {

  private static final long serialVersionUID = 1L;

  /** Where the player's sprite folders are, under the textures folder. */
  static final String FOLDER = "topdown/player/";

  private final transient Texture left = new Texture(FOLDER + "moveleft");
  private final transient Texture right = new Texture(FOLDER + "moveright");
  private final transient Texture forward = new Texture(FOLDER + "rest");
  private transient volatile Texture current = forward;
  private volatile int spriteX;
  private volatile int spriteY;

  /**
   * Brings the sprite up to date with the body it shows.
   *
   * @param mover the player's body in the cave
   */
  public void update(CaveMover mover) {
    Texture next;
    switch (mover.pose()) {
      case LEFT, CLIMBING_LEFT -> next = left;
      case RIGHT, CLIMBING_RIGHT -> next = right;
      default -> next = forward;
    }
    if (next == forward || mover.isMoving()) {
      next.tick();
    }
    current = next;
    spriteX = (int) Math.round(mover.feetX());
    spriteY = (int) Math.round(mover.feetY());
  }

  @Override
  public Iterator<Renderable> iterator() {
    return Collections.<Renderable>singletonList(this).iterator();
  }

  @Override
  public Texture getTexture() {
    return current;
  }

  @Override
  public int getX() {
    return spriteX;
  }

  @Override
  public int getY() {
    return spriteY;
  }

  @Override
  public int getWidth() {
    return current.getWidth();
  }

  @Override
  public int getHeight() {
    return current.getHeight();
  }
}
