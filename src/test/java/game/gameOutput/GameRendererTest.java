package game.gameOutput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.gameOutput.GameViewBoundary.Loop;
import game.gameOutput.GameViewBoundary.OneShot;
import game.gameOutput.GameViewBoundary.ScreenItem;
import game.interactor.gameModel.Texture;
import game.interactor.outputData.RenderableData;
import game.interactor.outputData.RenderableData.Layer;
import game.interactor.outputData.RenderableData.LoopingSound;
import game.interactor.outputData.RenderableData.RenderableItem;
import game.interactor.outputData.RenderableData.SoundEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

class GameRendererTest {

  private static final int WIDTH = 800;
  private static final int HEIGHT = 600;

  /** A view that remembers every frame it is shown. */
  private static final class FakeView implements GameViewBoundary {
    private final List<List<ScreenItem>> shown = new CopyOnWriteArrayList<>();

    @Override
    public void show(List<ScreenItem> frame) {
      shown.add(frame);
    }
  }

  /** A renderer handing frames to the view through the given executor. */
  private static GameRenderer renderer(GameViewBoundary view, Executor swing) {
    return new GameRenderer(view, WIDTH, HEIGHT, swing);
  }

  /** A view that remembers every sound it is asked to play, and ignores the pictures. */
  private static final class FakeAudio implements GameViewBoundary {
    private final List<List<Loop>> loops = new CopyOnWriteArrayList<>();
    private final List<OneShot> played = new CopyOnWriteArrayList<>();

    @Override
    public void show(List<ScreenItem> frame) {}

    @Override
    public void playSound(List<Loop> loops, List<OneShot> now) {
      this.loops.add(loops);
      played.addAll(now);
    }
  }

  private static RenderableData sounding(List<LoopingSound> loops, SoundEvent... events) {
    return new RenderableData(0, 0, List.of(), loops, List.of(events));
  }

  private static Texture texture(int width, int height) {
    return new Texture(
        new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), width, height);
  }

  private static RenderableData world(double cameraX, double cameraY, RenderableItem... items) {
    return new RenderableData(cameraX, cameraY, List.of(Layer.inWorld(List.of(items))));
  }

  private static ScreenItem only(RenderableData data) {
    List<ScreenItem> frame = GameRenderer.toScreen(data, WIDTH, HEIGHT);
    assertEquals(1, frame.size());
    return frame.get(0);
  }

  @Test
  void somethingAtTheCameraIsInTheMiddleOfTheScreen() {
    Texture image = texture(20, 30);
    ScreenItem item = only(world(100, 200, new RenderableItem(image, 100, 200)));
    // Its middle at the screen's middle, standing on the screen's middle line.
    assertEquals(WIDTH / 2 - 10, item.x());
    assertEquals(HEIGHT / 2 - 30, item.y());
    assertEquals(20, item.width());
    assertEquals(30, item.height());
    assertSame(image, item.texture());
  }

  @Test
  void rightInTheWorldIsRightOnScreenAndUpInTheWorldIsUpOnScreen() {
    Texture image = texture(20, 30);
    ScreenItem atCamera = only(world(0, 0, new RenderableItem(image, 0, 0)));
    ScreenItem rightAndUp = only(world(0, 0, new RenderableItem(image, 50, 70)));
    assertEquals(atCamera.x() + 50, rightAndUp.x());
    assertEquals(atCamera.y() - 70, rightAndUp.y());
    // Moving the camera the same way puts it back in the middle.
    ScreenItem followed = only(world(50, 70, new RenderableItem(image, 50, 70)));
    assertEquals(atCamera.x(), followed.x());
    assertEquals(atCamera.y(), followed.y());
  }

  @Test
  void theCameraIsRoundedToWholePixelsOnceForEveryone() {
    Texture image = texture(10, 10);
    RenderableData data =
        world(100.6, -0.4, new RenderableItem(image, 100, 0), new RenderableItem(image, 120, 0));
    List<ScreenItem> frame = GameRenderer.toScreen(data, WIDTH, HEIGHT);
    // A camera at 100.6 counts as 101 for both, so they stay exactly 20 pixels apart.
    assertEquals(WIDTH / 2 - 5 - 1, frame.get(0).x());
    assertEquals(20, frame.get(1).x() - frame.get(0).x());
    assertEquals(HEIGHT / 2 - 10, frame.get(0).y());
  }

  @Test
  void screenLayersStayPutWhateverTheCamera() {
    Texture overlay = texture(WIDTH, HEIGHT);
    RenderableData data =
        new RenderableData(
            12_345, -6_789, List.of(Layer.onScreen(List.of(new RenderableItem(overlay, 0, 0)))));
    ScreenItem item = only(data);
    assertEquals(0, item.x());
    assertEquals(0, item.y());
  }

  @Test
  void layersAndItemsAreDrawnInOrderFromTheBack() {
    Texture back = texture(10, 10);
    Texture middle = texture(10, 10);
    Texture front = texture(10, 10);
    Texture overlay = texture(10, 10);
    RenderableData data =
        new RenderableData(
            0,
            0,
            List.of(
                Layer.inWorld(List.of(new RenderableItem(back, 0, 0))),
                Layer.inWorld(
                    List.of(new RenderableItem(middle, 5, 0), new RenderableItem(front, 9, 0))),
                Layer.onScreen(List.of(new RenderableItem(overlay, 3, 3)))));
    List<ScreenItem> frame = GameRenderer.toScreen(data, WIDTH, HEIGHT);
    assertEquals(4, frame.size());
    assertSame(back, frame.get(0).texture());
    assertSame(middle, frame.get(1).texture());
    assertSame(front, frame.get(2).texture());
    assertSame(overlay, frame.get(3).texture());
  }

  @Test
  void whatIsOffTheScreenOrHasNoImageIsLeftOut() {
    Texture image = texture(100, 100);
    RenderableData data =
        world(
            0,
            0,
            new RenderableItem(image, WIDTH, 0), // just off the right edge
            new RenderableItem(image, WIDTH / 2 + 40, 0), // poking in from the right
            new RenderableItem(image, 0, -HEIGHT), // far below
            new RenderableItem(null, 0, 0));
    List<ScreenItem> frame = GameRenderer.toScreen(data, WIDTH, HEIGHT);
    assertEquals(1, frame.size());
    assertEquals(WIDTH - 10, frame.get(0).x(), "only its left 10 pixels show, but it is kept");
  }

  @Test
  void nothingIsShownBeforeTheFirstFrame() {
    FakeView view = new FakeView();
    GameRenderer renderer = renderer(view, Runnable::run);
    renderer.renderFrame();
    assertTrue(view.shown.isEmpty());
  }

  @Test
  void theLatestFrameIsDrawnAgainUntilANewOneArrives() {
    FakeView view = new FakeView();
    GameRenderer renderer = renderer(view, Runnable::run);
    Texture first = texture(10, 10);
    Texture second = texture(10, 10);
    renderer.submitSnapshot(world(0, 0, new RenderableItem(first, 0, 0)));
    renderer.renderFrame();
    renderer.renderFrame();
    renderer.submitSnapshot(world(0, 0, new RenderableItem(second, 0, 0)));
    renderer.renderFrame();
    assertEquals(3, view.shown.size());
    assertSame(first, view.shown.get(1).get(0).texture(), "drawn again while nothing new came");
    assertSame(second, view.shown.get(2).get(0).texture());
  }

  @Test
  void aFrameIsSkippedWhileTheViewIsStillBusyWithTheLastOne() {
    FakeView view = new FakeView();
    Queue<Runnable> swing = new ArrayDeque<>();
    GameRenderer renderer = renderer(view, swing::add);
    renderer.submitSnapshot(world(0, 0, new RenderableItem(texture(10, 10), 0, 0)));
    renderer.renderFrame();
    renderer.renderFrame();
    renderer.renderFrame();
    assertEquals(1, swing.size(), "only one hand-over waits; the others were skipped");
    swing.poll().run();
    assertEquals(1, view.shown.size());
    renderer.renderFrame();
    assertEquals(1, swing.size(), "once the view has caught up, drawing carries on");
  }

  @Test
  void aViewThatFailsOnceDoesNotStopDrawing() {
    List<List<ScreenItem>> shown = new ArrayList<>();
    boolean[] failed = {false};
    GameViewBoundary flaky =
        frame -> {
          if (!failed[0]) {
            failed[0] = true;
            throw new IllegalStateException("a bad frame");
          }
          shown.add(frame);
        };
    GameRenderer renderer = renderer(flaky, Runnable::run);
    renderer.submitSnapshot(world(0, 0, new RenderableItem(texture(10, 10), 0, 0)));
    renderer.renderFrame();
    renderer.renderFrame();
    assertEquals(1, shown.size());
  }

  @Test
  void theThreadDrawsSteadilyAndCanPauseAndResume() throws InterruptedException {
    FakeView view = new FakeView();
    GameRenderer renderer = renderer(view, Runnable::run);
    renderer.submitSnapshot(world(0, 0, new RenderableItem(texture(10, 10), 0, 0)));
    renderer.start();
    Thread.sleep(500);
    int afterHalfASecond = view.shown.size();
    // About 30 frames at 60 a second; allow a slow test machine plenty of room either way.
    assertTrue(afterHalfASecond >= 10 && afterHalfASecond <= 40, "drew " + afterHalfASecond);
    renderer.pause();
    assertTrue(renderer.isPaused());
    Thread.sleep(100);
    int paused = view.shown.size();
    Thread.sleep(300);
    assertEquals(paused, view.shown.size(), "nothing is drawn while paused");
    renderer.resume();
    Thread.sleep(300);
    assertTrue(view.shown.size() > paused + 5, "drawing carries on after resuming");
    assertThrows(IllegalStateException.class, renderer::start);
  }

  @Test
  void loopingSoundsReachTheAudioEveryFrame() {
    FakeAudio audio = new FakeAudio();
    GameRenderer renderer =
        new GameRenderer(audio, WIDTH, HEIGHT, Runnable::run);
    renderer.submitSnapshot(sounding(List.of(new LoopingSound("weather/light-rain", 0.4))));
    renderer.renderFrame();
    renderer.renderFrame();
    renderer.submitSnapshot(sounding(List.of()));
    renderer.renderFrame();
    assertEquals(3, audio.loops.size());
    assertEquals(List.of(new Loop("weather/light-rain", 0.4)), audio.loops.get(1));
    assertTrue(audio.loops.get(2).isEmpty(), "a loop left out of a frame stops");
  }

  @Test
  void eachSoundEventIsPlayedOnceThoughItIsInSeveralFrames() {
    FakeAudio audio = new FakeAudio();
    GameRenderer renderer =
        new GameRenderer(audio, WIDTH, HEIGHT, Runnable::run);
    SoundEvent thunder = new SoundEvent(5, "weather/thunder-close", 0.9);
    renderer.submitSnapshot(sounding(List.of(), thunder));
    renderer.renderFrame();
    renderer.renderFrame();
    // A newer frame still listing the same thunder, and a new one.
    renderer.submitSnapshot(
        sounding(List.of(), thunder, new SoundEvent(6, "weather/thunder-rumble", 0.5)));
    renderer.renderFrame();
    assertEquals(
        List.of(
            new OneShot("weather/thunder-close", 0.9), new OneShot("weather/thunder-rumble", 0.5)),
        audio.played);
  }

  @Test
  void aSoundEventInAFrameThatWasNeverDrawnIsStillPlayedFromTheNext() {
    FakeAudio audio = new FakeAudio();
    GameRenderer renderer =
        new GameRenderer(audio, WIDTH, HEIGHT, Runnable::run);
    SoundEvent first = new SoundEvent(1, "a", 1);
    renderer.submitSnapshot(sounding(List.of(), first));
    // Replaced before it was ever drawn; the newer frame still lists the event.
    renderer.submitSnapshot(sounding(List.of(), first, new SoundEvent(2, "b", 1)));
    renderer.renderFrame();
    assertEquals(List.of(new OneShot("a", 1), new OneShot("b", 1)), audio.played);
  }

  @Test
  void soundCarriesOnWhileTheViewIsBusy() {
    FakeAudio audio = new FakeAudio();
    Queue<Runnable> swing = new ArrayDeque<>();
    GameRenderer renderer = new GameRenderer(audio, WIDTH, HEIGHT, swing::add);
    renderer.submitSnapshot(sounding(List.of(), new SoundEvent(1, "a", 1)));
    renderer.renderFrame();
    renderer.submitSnapshot(sounding(List.of(), new SoundEvent(2, "b", 1)));
    renderer.renderFrame();
    assertEquals(1, swing.size(), "the picture was skipped");
    assertEquals(2, audio.played.size(), "but the sound was not");
  }

  @Test
  void loopsFallSilentWhilePaused() throws InterruptedException {
    FakeAudio audio = new FakeAudio();
    GameRenderer renderer =
        new GameRenderer(audio, WIDTH, HEIGHT, Runnable::run);
    renderer.submitSnapshot(sounding(List.of(new LoopingSound("rain", 1))));
    renderer.start();
    Thread.sleep(200);
    renderer.pause();
    Thread.sleep(200);
    List<Loop> last = audio.loops.get(audio.loops.size() - 1);
    assertTrue(last.isEmpty(), "silent while paused");
    renderer.resume();
    Thread.sleep(200);
    assertEquals(1, audio.loops.get(audio.loops.size() - 1).size(), "the rain comes back");
  }

  @Test
  void aFailingSoundDoesNotStopTheDrawing() {
    List<List<ScreenItem>> shown = new ArrayList<>();
    GameViewBoundary broken =
        new GameViewBoundary() {
          @Override
          public void show(List<ScreenItem> frame) {
            shown.add(frame);
          }

          @Override
          public void playSound(List<Loop> loops, List<OneShot> now) {
            throw new IllegalStateException("no sound device");
          }
        };
    GameRenderer renderer = new GameRenderer(broken, WIDTH, HEIGHT, Runnable::run);
    renderer.submitSnapshot(world(0, 0, new RenderableItem(texture(10, 10), 0, 0)));
    renderer.renderFrame();
    assertEquals(1, shown.size());
  }
}

