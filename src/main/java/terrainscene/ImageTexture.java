package terrainscene;

import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import jswing.Texture;

/**
 * A {@link Texture} showing an image made in memory, such as a painted chunk or the weather.
 *
 * <p>The game's {@code Texture} can only load image files, so this adapter (the Adapter pattern)
 * lets the renderer show a chunk image without changing that class. It is a stopgap: once {@code
 * Texture} can be built from an image directly, this class can be removed.
 */
public final class ImageTexture extends Texture {

  /** A small existing texture folder the parent constructor can load; its frames are unused. */
  private static final String UNUSED_PARENT_FOLDER = TileRenderable.TEXTURE_FOLDER + "air";

  private final JLabel label;
  private final int width;
  private final int height;

  /**
   * Wraps an image. Call on the Swing thread, since it creates a Swing component.
   *
   * @param image the image to show
   */
  public ImageTexture(BufferedImage image) {
    super(UNUSED_PARENT_FOLDER);
    this.label = new JLabel(new ImageIcon(image));
    this.width = image.getWidth();
    this.height = image.getHeight();
  }

  /** Does nothing: a chunk image is not animated. */
  @Override
  public void tick() {}

  @Override
  public JLabel getContent() {
    return label;
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public int getHeight() {
    return height;
  }
}
