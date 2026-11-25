package terrainscene;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * A looping animation of small waves moving across open water, drawn over the water's colour.
 *
 * <p>Each frame is a square tile that repeats seamlessly in every direction. The water's surface
 * is the sum of many small waves, each a whole number of times across the tile (so the tile wraps)
 * and moving a whole number of times through the loop (so the animation wraps). Their crests run
 * mostly sideways, as rippled water seen at an angle does, and they move both ways, so the surface
 * shimmers in place rather than drifting off in one direction. Bigger waves move faster, as on
 * real water. Each spot is then lightened where the surface tilts toward the camera and catches
 * the sky, which is what shows up as the ripples.
 *
 * <p>Frames are see-through apart from those highlights, so the water's own colour shows through.
 * They are worked out once, then reused for the whole game.
 */
final class WaterRipples {

  /** How wide and tall one tile of ripples is, in pixels. */
  static final int TILE_PX = 256;

  /** The size of each step of the ripples, in pixels. */
  static final int TEXEL_PX = 2;

  /** How many frames the loop has. */
  static final int FRAMES = 128;

  /** How long the loop lasts, in seconds. */
  static final double LOOP_SECONDS = 8;

  /** The colour where a ripple catches the sky. */
  static final int LIGHT_RGB = 0xC8D4E6;

  /** How opaque the brightest highlights are, from 0 to 1. */
  static final double LIGHT_OPACITY = 0.30;

  /**
   * How steep the surface must be toward the camera, in standard deviations, before it is lit at
   * all; gentler slopes, and slopes facing away, leave the water's own colour showing.
   */
  static final double THRESHOLD = 0.35;

  /** How steep the surface must be toward the camera, in standard deviations, to be fully lit. */
  static final double FULL = 2.2;

  /** How many larger waves make up the surface. */
  static final int SWELLS = 18;

  /** How many smaller, quicker waves are added on top. */
  static final int CHOP = 22;

  private static final int TEXELS = TILE_PX / TEXEL_PX;

  /** One wave: how many times it crosses the tile each way, its height, speed and phase. */
  private record Wave(int across, int down, double amplitude, int loops, double phase) {}

  /** The seed of the game's water, so every sheet of ripples shows the same waves. */
  static final long SEED = 1L;

  /** The game's one animation, worked out once and shared; {@code null} until first asked for. */
  private static CompletableFuture<WaterRipples> shared;

  private final BufferedImage[] frames;

  private WaterRipples(BufferedImage[] frames) {
    this.frames = frames;
  }

  /**
   * Works out every frame of the animation. Takes a fraction of a second, so call it away from
   * the Swing thread.
   *
   * @param seed picks the waves, so the same seed always gives the same water
   * @return the animation
   */
  static WaterRipples create(long seed) {
    List<Wave> waves = waves(new Random(seed));
    double spread = slopeSpread(waves);
    BufferedImage[] frames = new BufferedImage[FRAMES];
    for (int frame = 0; frame < FRAMES; frame++) {
      frames[frame] = paintFrame(waves, (double) frame / FRAMES, spread);
    }
    return new WaterRipples(frames);
  }

  /**
   * Returns the game's ripples, starting to work them out on a background thread the first time,
   * so that every sheet of ripples shares one animation instead of each making its own.
   *
   * @return the ripples, once they are ready
   */
  static synchronized CompletableFuture<WaterRipples> shared() {
    if (shared == null) {
      shared = new CompletableFuture<>();
      CompletableFuture<WaterRipples> result = shared;
      Thread thread =
          new Thread(
              () -> {
                try {
                  result.complete(create(SEED));
                } catch (RuntimeException e) {
                  result.completeExceptionally(e);
                }
              },
              "water-ripples");
      thread.setDaemon(true);
      thread.start();
    }
    return shared;
  }

  /**
   * Returns the frame to show at a moment.
   *
   * @param seconds any running time, in seconds
   * @return a {@link #TILE_PX}-square frame, see-through apart from the ripples
   */
  BufferedImage frameAt(double seconds) {
    double loops = seconds / LOOP_SECONDS;
    int frame = (int) Math.floor((loops - Math.floor(loops)) * FRAMES);
    return frames[Math.min(FRAMES - 1, frame)];
  }

  /**
   * Returns a frame by its number.
   *
   * @param frame from 0 to {@link #FRAMES} - 1
   * @return the frame
   */
  BufferedImage frame(int frame) {
    return frames[frame];
  }

  /** Picks the waves: larger swells, then smaller chop on top. */
  private static List<Wave> waves(Random random) {
    List<Wave> waves = new ArrayList<>();
    addWaves(waves, random, SWELLS, 5, 12);
    addWaves(waves, random, CHOP, 13, 30);
    return waves;
  }

  /**
   * Adds waves crossing the tile from {@code fewest} to {@code most} times top to bottom, and up
   * to about a quarter as often side to side, so their crests are mostly sideways.
   */
  private static void addWaves(
      List<Wave> waves, Random random, int count, int fewest, int most) {
    for (int i = 0; i < count; i++) {
      int down = fewest + random.nextInt(most - fewest + 1);
      int sideways = (down + 2) / 4;
      int across = random.nextInt(2 * sideways + 1) - sideways;
      double frequency = Math.hypot(across, down);
      // Smaller waves are lower, so the big ones set the pattern and the small ones add texture.
      double amplitude = Math.pow(frequency, -1.25) * (0.7 + 0.6 * random.nextDouble());
      // Deep-water waves move at a speed that grows with their size: frequency grows as the
      // square root of the wavenumber. Half move each way.
      int loops = Math.max(1, (int) Math.round(0.75 * Math.sqrt(frequency)));
      if (random.nextBoolean()) {
        loops = -loops;
      }
      waves.add(new Wave(across, down, amplitude, loops, random.nextDouble() * 2 * Math.PI));
    }
  }

  /**
   * Returns how steep the surface is toward the camera at a spot, in no particular unit: the
   * slope top to bottom, with a little of the slope side to side.
   */
  private static double slope(List<Wave> waves, double u, double v, double time) {
    double down = 0;
    double across = 0;
    for (Wave wave : waves) {
      double angle =
          2 * Math.PI * (wave.across() * u + wave.down() * v - wave.loops() * time)
              + wave.phase();
      double rate = wave.amplitude() * Math.cos(angle);
      down += rate * wave.down();
      across += rate * wave.across();
    }
    return down + 0.15 * across;
  }

  /** Returns the standard deviation of the slope over the first frame, to scale the shading. */
  private static double slopeSpread(List<Wave> waves) {
    double sum = 0;
    for (int x = 0; x < TEXELS; x++) {
      for (int y = 0; y < TEXELS; y++) {
        double slope = slope(waves, (double) x / TEXELS, (double) y / TEXELS, 0);
        sum += slope * slope;
      }
    }
    return Math.sqrt(sum / (TEXELS * TEXELS));
  }

  /** Paints one frame, a share {@code time} of the way through the loop. */
  private static BufferedImage paintFrame(List<Wave> waves, double time, double spread) {
    int[] pixels = new int[TILE_PX * TILE_PX];
    for (int x = 0; x < TEXELS; x++) {
      for (int y = 0; y < TEXELS; y++) {
        double slope = slope(waves, (double) x / TEXELS, (double) y / TEXELS, time) / spread;
        int colour = shade(slope);
        for (int row = y * TEXEL_PX; row < (y + 1) * TEXEL_PX; row++) {
          for (int column = x * TEXEL_PX; column < (x + 1) * TEXEL_PX; column++) {
            pixels[row * TILE_PX + column] = colour;
          }
        }
      }
    }
    BufferedImage image = new BufferedImage(TILE_PX, TILE_PX, BufferedImage.TYPE_INT_ARGB);
    image.setRGB(0, 0, TILE_PX, TILE_PX, pixels, 0, TILE_PX);
    return image;
  }

  /**
   * Returns the overlay for a slope, in standard deviations: a highlight where it tilts toward the
   * camera, nothing where it is gentle or tilts away.
   */
  static int shade(double slope) {
    double strength = smoothstep(THRESHOLD, FULL, slope);
    if (strength <= 0) {
      return 0;
    }
    int alpha = (int) Math.round(strength * LIGHT_OPACITY * 255);
    return alpha << 24 | LIGHT_RGB;
  }

  private static double smoothstep(double from, double to, double value) {
    double t = Math.max(0, Math.min(1, (value - from) / (to - from)));
    return t * t * (3 - 2 * t);
  }
}
