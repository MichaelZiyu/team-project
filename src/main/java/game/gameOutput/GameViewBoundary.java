package game.gameOutput;

import game.interactor.gameModel.Texture;
import java.util.List;

/**
 * Shows and sounds the game, as the renderer works it out: where each image goes on the screen,
 * and what should be heard.
 */
public interface GameViewBoundary {

  /**
   * One image placed on the screen.
   *
   * @param texture the image
   * @param x its left edge, in pixels from the left of the screen
   * @param y its top edge, in pixels from the top of the screen
   * @param width how wide to draw it, in pixels
   * @param height how tall to draw it, in pixels
   */
  record ScreenItem(Texture texture, int x, int y, int width, int height) {}

  /**
   * A sound that plays on and on.
   *
   * @param name the sound, named by its file under the sound folder without the ending
   * @param volume how loud, from 0 (silent) to 1 (full)
   */
  record Loop(String name, double volume) {}

  /**
   * A sound to play once, now.
   *
   * @param name the sound, named by its file under the sound folder without the ending
   * @param volume how loud, from 0 (silent) to 1 (full)
   */
  record OneShot(String name, double volume) {}

  /**
   * Shows a frame. Called on the Swing thread.
   *
   * @param frame the images to draw, from the back to the front; never changed afterwards
   */
  void show(List<ScreenItem> frame);

  /**
   * Brings the sound up to date: every loop listed plays at its volume, every loop not listed
   * stops, and each one-shot starts playing. Called once each frame, on the renderer's thread,
   * even when the frame's picture is skipped. A view with no sound can leave this out: by default
   * it plays nothing.
   *
   * @param loops the loops to be heard now; each name at most once
   * @param now the sounds to start playing now, each heard once
   */
  default void playSound(List<Loop> loops, List<OneShot> now) {}
}
