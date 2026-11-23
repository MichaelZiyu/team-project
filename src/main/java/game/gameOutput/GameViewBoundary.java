package game.gameOutput;

import game.interactor.gameModel.Texture;
import java.util.List;

/** Shows frames the renderer has worked out: where each image goes on the screen. */
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
   * Shows a frame. Called on the Swing thread.
   *
   * @param frame the images to draw, from the back to the front; never changed afterwards
   */
  void show(List<ScreenItem> frame);
}
