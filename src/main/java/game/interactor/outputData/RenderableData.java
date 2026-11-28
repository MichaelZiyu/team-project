package game.interactor.outputData;

import game.interactor.gameModel.Texture;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * One frame of the game, ready for the renderer: where the camera is, everything to draw, in layers
 * from the back to the front, and what should be heard.
 *
 * <p>Layers are drawn in order, each over the ones before it, so the first layer is at the back.
 * The usual order is: background, terrain, interactable things, characters, status effects, stats,
 * filters (such as weather), collision boxes, then the game's UI. Within a layer, items are drawn
 * in order too, so whoever builds a layer sorts it (for example, characters further away first).
 *
 * <p>Most layers are placed in the world: each item's {@code x} is the horizontal middle of its
 * image and {@code y} the bottom edge, in world pixels with y pointing up, and the renderer moves
 * them with the camera. A layer can instead be fixed to the screen, like weather over the whole
 * view or a menu: then each item's {@code x} and {@code y} are the top-left corner of its image on
 * screen, in pixels with y pointing down, and the camera does not move it.
 *
 * <p>Sound comes in two kinds. A {@link LoopingSound}, such as rain, plays on and on: every frame
 * lists the looping sounds that should be heard and how loudly, and a sound that a frame leaves out
 * should stop. A {@link SoundEvent}, such as a thunderclap, plays once. Frames are not guaranteed
 * to be used one by one (one can be drawn twice, or skipped for a newer one), so each event has a
 * number of its own, and stays in the frames for {@link #SOUND_EVENT_SECONDS} after it happens; the
 * player plays each number once, so an event is never heard twice, nor missed. Sounds are named by
 * their file under the game's sound folder, without the ending: {@code "weather/light-rain"} is
 * {@code sounds/weather/light-rain.wav}.
 *
 * <p>A frame must not change once it has been handed over, since the renderer draws it on its own
 * thread: build a new frame every time rather than editing an old one, and never repaint an image
 * or advance a texture's animation after its frame has been handed over.
 *
 * @param cameraX the camera's x position in the world, in pixels: the middle of the screen
 * @param cameraY the camera's y position in the world, in pixels: the middle of the screen
 * @param layers what to draw, from the back to the front
 * @param loopingSounds the looping sounds to be heard now; each sound at most once
 * @param soundEvents the sounds played once that happened in the last {@link
 *     #SOUND_EVENT_SECONDS}; each number at most once
 */
public record RenderableData(
    double cameraX,
    double cameraY,
    List<Layer> layers,
    List<LoopingSound> loopingSounds,
    List<SoundEvent> soundEvents) {

  /**
   * How long a sound played once stays in the frames after it happens, in seconds: long enough
   * that a frame or two skipped by the renderer cannot lose it.
   */
  public static final double SOUND_EVENT_SECONDS = 0.25;

  /** The last sound event number handed out. */
  private static final AtomicLong LAST_SOUND_EVENT = new AtomicLong();

  /**
   * Returns a number for a new sound event: one no other sound event has had, and higher than all
   * before it. Every part of the game making sound events takes its numbers from here, so they
   * never clash. Safe to call from any thread.
   *
   * @return the number
   */
  public static long nextSoundEventNumber() {
    return LAST_SOUND_EVENT.incrementAndGet();
  }

  /**
   * Creates a frame. The lists are copied, so later changes to them do not affect the frame.
   *
   * @throws IllegalArgumentException if a looping sound is listed twice, or two sound events share
   *     a number
   */
  public RenderableData {
    layers = List.copyOf(layers);
    loopingSounds = List.copyOf(loopingSounds);
    soundEvents = List.copyOf(soundEvents);
    Set<String> looping = new HashSet<>();
    for (LoopingSound sound : loopingSounds) {
      if (!looping.add(sound.name())) {
        throw new IllegalArgumentException("looping sound listed twice: " + sound.name());
      }
    }
    Set<Long> numbers = new HashSet<>();
    for (SoundEvent event : soundEvents) {
      if (!numbers.add(event.number())) {
        throw new IllegalArgumentException("two sound events numbered " + event.number());
      }
    }
  }

  /**
   * Creates a silent frame: pictures only.
   *
   * @param cameraX the camera's x position in the world, in pixels
   * @param cameraY the camera's y position in the world, in pixels
   * @param layers what to draw, from the back to the front
   */
  public RenderableData(double cameraX, double cameraY, List<Layer> layers) {
    this(cameraX, cameraY, layers, List.of(), List.of());
  }

  /**
   * A sound that plays on and on, such as rain, for as long as frames list it.
   *
   * @param name the sound, named by its file under the sound folder without the ending
   * @param volume how loud, from 0 (silent) to 1 (full)
   */
  public record LoopingSound(String name, double volume) {

    /**
     * Creates a looping sound.
     *
     * @throws IllegalArgumentException if the name is blank or the volume out of range
     */
    public LoopingSound {
      checkName(name);
      checkVolume(volume);
    }
  }

  /**
   * A sound that plays once, such as a thunderclap.
   *
   * @param number a number no other sound event of the game has, from {@link
   *     #nextSoundEventNumber}
   * @param name the sound, named by its file under the sound folder without the ending
   * @param volume how loud, from 0 (silent) to 1 (full)
   */
  public record SoundEvent(long number, String name, double volume) {

    /**
     * Creates a sound event.
     *
     * @throws IllegalArgumentException if the name is blank or the volume out of range
     */
    public SoundEvent {
      checkName(name);
      checkVolume(volume);
    }
  }

  private static void checkName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("a sound needs a name");
    }
  }

  private static void checkVolume(double volume) {
    if (!(volume >= 0 && volume <= 1)) {
      throw new IllegalArgumentException("volume must be in [0, 1]: " + volume);
    }
  }

  /**
   * One layer of a frame.
   *
   * @param screenFixed {@code true} if its items are placed on the screen, {@code false} if in the
   *     world
   * @param items what to draw in this layer, from the back to the front
   */
  public record Layer(boolean screenFixed, List<RenderableItem> items) {

    /** Creates a layer. The list of items is copied, so later changes to it do not affect it. */
    public Layer {
      items = List.copyOf(items);
    }

    /**
     * Returns a layer placed in the world.
     *
     * @param items what to draw, from the back to the front
     * @return the layer
     */
    public static Layer inWorld(List<RenderableItem> items) {
      return new Layer(false, items);
    }

    /**
     * Returns a layer fixed to the screen.
     *
     * @param items what to draw, from the back to the front
     * @return the layer
     */
    public static Layer onScreen(List<RenderableItem> items) {
      return new Layer(true, items);
    }
  }

  /**
   * One image to draw.
   *
   * @param texture the image, at its own width and height
   * @param x in a world layer, the image's horizontal middle in world pixels; in a screen layer,
   *     its left edge on screen
   * @param y in a world layer, the image's bottom edge in world pixels (y up); in a screen layer,
   *     its top edge on screen (y down)
   */
  public record RenderableItem(Texture texture, int x, int y) {}
}
