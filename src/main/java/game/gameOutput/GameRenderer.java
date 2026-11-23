package game.gameOutput;

import game.gameOutput.GameAudioBoundary.Loop;
import game.gameOutput.GameAudioBoundary.OneShot;
import game.gameOutput.GameViewBoundary.ScreenItem;
import game.interactor.gameModel.Texture;
import game.interactor.outputData.RenderableData;
import game.interactor.outputData.RenderableData.Layer;
import game.interactor.outputData.RenderableData.LoopingSound;
import game.interactor.outputData.RenderableData.RenderableItem;
import game.interactor.outputData.RenderableData.SoundEvent;
import game.settings.GameSettings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import javax.swing.SwingUtilities;

/**
 * Draws the game: a steady {@link GameSettings#FPS} times a second, on its own thread, it takes the
 * latest frame the game has handed over ({@link #submitSnapshot}), works out where everything goes
 * on the screen, and passes that to the view.
 *
 * <p>Drawing does not wait for the game: if no new frame has arrived since the last draw, the same
 * one is drawn again, and if the view has not finished showing the last frame yet, this one is
 * skipped rather than queued, so frames never pile up.
 *
 * <p>Positions in the world become positions on the screen with the camera at the screen's middle
 * (see {@link RenderableData}): an item's middle is as far right of the screen's middle as it is
 * right of the camera, and the y direction flips, since world y points up and screen y down. The
 * camera is rounded to whole pixels once per frame, so everything moves together. Items entirely
 * off the screen are left out. Layers fixed to the screen are drawn where they are.
 *
 * <p>Each draw also brings the sound up to date ({@link GameAudioBoundary}), even when the picture
 * is skipped: the frame's looping sounds at their volumes, and its sounds played once. A frame
 * keeps a sound played once for a moment after it happens, so the same one can appear in several
 * frames; each is played the first time its number is seen, and never again.
 *
 * <p>{@link #start} starts the drawing thread; {@link #pause} and {@link #resume} stop and restart
 * the drawing, for example while a menu is open. While paused, looping sounds are silent.
 */
public final class GameRenderer implements GameRendererBoundary {

  private static final long NANOS_PER_SECOND = 1_000_000_000L;

  private final GameViewBoundary view;
  private final GameAudioBoundary audio;
  private final int viewWidthPx;
  private final int viewHeightPx;
  private final Executor swing;
  private final AtomicReference<RenderableData> snapshot = new AtomicReference<>();
  private final AtomicBoolean viewBusy = new AtomicBoolean();
  private final Object pauseLock = new Object();
  private boolean paused;
  private Thread thread;

  /** The highest sound event number played so far; only touched by the drawing thread. */
  private long lastSoundEvent = Long.MIN_VALUE;

  /**
   * Creates a silent renderer drawing into a view of a given size.
   *
   * @param view where frames are shown
   * @param viewWidthPx the view's width, in pixels
   * @param viewHeightPx the view's height, in pixels
   */
  public GameRenderer(GameViewBoundary view, int viewWidthPx, int viewHeightPx) {
    this(view, GameAudioBoundary.SILENT, viewWidthPx, viewHeightPx);
  }

  /**
   * Creates a renderer drawing into a view of a given size, and playing sound.
   *
   * @param view where frames are shown
   * @param audio where sound is played
   * @param viewWidthPx the view's width, in pixels
   * @param viewHeightPx the view's height, in pixels
   */
  public GameRenderer(
      GameViewBoundary view, GameAudioBoundary audio, int viewWidthPx, int viewHeightPx) {
    this(view, audio, viewWidthPx, viewHeightPx, SwingUtilities::invokeLater);
  }

  /**
   * Creates a renderer that hands frames to the view through a given executor; used by tests,
   * which run the hand-over themselves instead of on the Swing thread.
   *
   * @param view where frames are shown
   * @param audio where sound is played
   * @param viewWidthPx the view's width, in pixels
   * @param viewHeightPx the view's height, in pixels
   * @param swing runs the hand-over to the view; normally the Swing thread
   */
  GameRenderer(
      GameViewBoundary view,
      GameAudioBoundary audio,
      int viewWidthPx,
      int viewHeightPx,
      Executor swing) {
    this.view = view;
    this.audio = audio;
    this.viewWidthPx = viewWidthPx;
    this.viewHeightPx = viewHeightPx;
    this.swing = swing;
  }

  /**
   * Hands over the game's latest frame; it is drawn from the next draw on. Safe to call from any
   * thread.
   *
   * @param data the frame; it must not change afterwards (see {@link RenderableData})
   */
  @Override
  public void submitSnapshot(RenderableData data) {
    snapshot.set(data);
  }

  /**
   * Starts drawing, on a thread of its own.
   *
   * @throws IllegalStateException if it has already been started
   */
  public synchronized void start() {
    if (thread != null) {
      throw new IllegalStateException("the renderer has already been started");
    }
    thread = new Thread(this::run, "renderer");
    thread.setDaemon(true);
    thread.start();
  }

  /** Stops drawing until {@link #resume} is called; the last frame drawn stays on the screen. */
  public void pause() {
    synchronized (pauseLock) {
      paused = true;
    }
  }

  /** Carries on drawing after {@link #pause}. */
  public void resume() {
    synchronized (pauseLock) {
      paused = false;
      pauseLock.notifyAll();
    }
  }

  /**
   * Returns whether drawing is paused.
   *
   * @return {@code true} between {@link #pause} and {@link #resume}
   */
  public boolean isPaused() {
    synchronized (pauseLock) {
      return paused;
    }
  }

  /** Draws a steady {@link GameSettings#FPS} times a second, waiting while paused. */
  private void run() {
    long frameNanos = NANOS_PER_SECOND / GameSettings.FPS;
    long nextFrame = System.nanoTime();
    while (!Thread.currentThread().isInterrupted()) {
      if (waitWhilePaused()) {
        // Start timing afresh, rather than rushing to catch up the frames missed while paused.
        nextFrame = System.nanoTime();
      }
      renderFrame();
      nextFrame += frameNanos;
      long remaining;
      while ((remaining = nextFrame - System.nanoTime()) > 0) {
        LockSupport.parkNanos(remaining);
      }
    }
  }

  /** Waits while paused, with looping sounds silent; returns whether it had to wait. */
  private boolean waitWhilePaused() {
    synchronized (pauseLock) {
      boolean waited = false;
      while (paused) {
        if (!waited) {
          playQuietly(List.of(), List.of());
        }
        waited = true;
        try {
          pauseLock.wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return waited;
        }
      }
      return waited;
    }
  }

  /**
   * Draws once: brings the sound up to date with the latest frame, then works out its screen
   * positions and hands them to the view, unless the view is still busy with the last frame. Does
   * nothing before the first frame.
   */
  void renderFrame() {
    RenderableData data = snapshot.get();
    if (data == null) {
      return;
    }
    playQuietly(loopsOf(data), newSoundEvents(data));
    if (!viewBusy.compareAndSet(false, true)) {
      return;
    }
    try {
      List<ScreenItem> frame = toScreen(data, viewWidthPx, viewHeightPx);
      swing.execute(
          () -> {
            try {
              view.show(frame);
            } finally {
              viewBusy.set(false);
            }
          });
    } catch (RuntimeException e) {
      // A bad frame must not stop drawing for good: skip it and draw the next one.
      viewBusy.set(false);
      System.err.println("Could not draw a frame: " + e);
    }
  }

  /** Brings the sound up to date, never letting a sound problem stop the drawing. */
  private void playQuietly(List<Loop> loops, List<OneShot> now) {
    try {
      audio.update(loops, now);
    } catch (RuntimeException e) {
      System.err.println("Could not play sound: " + e);
    }
  }

  /** Returns a frame's looping sounds, for the audio. */
  private static List<Loop> loopsOf(RenderableData data) {
    List<Loop> loops = new ArrayList<>();
    for (LoopingSound sound : data.loopingSounds()) {
      loops.add(new Loop(sound.name(), sound.volume()));
    }
    return Collections.unmodifiableList(loops);
  }

  /**
   * Returns a frame's sounds played once that have not been played yet, in number order, and
   * remembers them as played.
   */
  List<OneShot> newSoundEvents(RenderableData data) {
    List<SoundEvent> fresh = new ArrayList<>();
    for (SoundEvent event : data.soundEvents()) {
      if (event.number() > lastSoundEvent) {
        fresh.add(event);
      }
    }
    fresh.sort(Comparator.comparingLong(SoundEvent::number));
    List<OneShot> now = new ArrayList<>();
    for (SoundEvent event : fresh) {
      now.add(new OneShot(event.name(), event.volume()));
      lastSoundEvent = event.number();
    }
    return Collections.unmodifiableList(now);
  }

  /**
   * Works out where a frame's images go on a screen of a given size, from the back to the front,
   * leaving out images with no texture and images entirely off the screen.
   *
   * @param data the frame
   * @param viewWidthPx the screen's width, in pixels
   * @param viewHeightPx the screen's height, in pixels
   * @return the images placed on the screen, in drawing order
   */
  static List<ScreenItem> toScreen(RenderableData data, int viewWidthPx, int viewHeightPx) {
    long cameraX = Math.round(data.cameraX());
    long cameraY = Math.round(data.cameraY());
    List<ScreenItem> frame = new ArrayList<>();
    for (Layer layer : data.layers()) {
      for (RenderableItem item : layer.items()) {
        Texture texture = item.texture();
        if (texture == null) {
          continue;
        }
        int width = texture.getWidth();
        int height = texture.getHeight();
        long left;
        long top;
        if (layer.screenFixed()) {
          left = item.x();
          top = item.y();
        } else {
          left = item.x() - cameraX + viewWidthPx / 2 - width / 2;
          top = viewHeightPx / 2 - (item.y() - cameraY) - height;
        }
        if (left + width > 0 && left < viewWidthPx && top + height > 0 && top < viewHeightPx) {
          frame.add(new ScreenItem(texture, (int) left, (int) top, width, height));
        }
      }
    }
    return Collections.unmodifiableList(frame);
  }
}
