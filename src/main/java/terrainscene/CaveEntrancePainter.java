package terrainscene;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import terrain.BlockType;
import terrain.CaveEntrance;
import terrain.TerrainCoordinates;

/**
 * Paints what a cave entrance looks like from above: a jagged opening in the ground, rimmed with
 * the soil under the surface, whose rock walls fade away into darkness.
 *
 * <p>The game's camera looks down at an angle, from the south, so of the opening's walls it is the
 * far (north) one that shows: the darkness takes longer to close in from the top of the opening
 * than from the bottom. The opening's outline is a rough oval, made irregular by a few random
 * bumps chosen from the entrance's seed, so no two entrances look the same, but each always looks
 * the same. It is drawn in steps of {@link #TEXEL_PX} pixels and a few shades, like the terrain's
 * textures. Paintings are kept, so each entrance is painted once. Safe to use from several threads
 * at once.
 */
final class CaveEntrancePainter {

  /** The size of each step of the painting, in pixels: one pixel of a 16-pixel texture. */
  static final int TEXEL_PX = 4;

  /** How far in from the edge, as a share of the way to the middle, the soil rim reaches. */
  static final double RIM_DEPTH = 0.12;

  /** How far in from the edge, as a share of the way to the middle, it is fully dark. */
  static final double DARK_DEPTH = 0.7;

  /** How many shades the walls fade through, for a pixel-art look. */
  static final int SHADES = 6;

  /** How much longer the far wall takes to fade than the opening's middle. */
  static final double FAR_WALL_STRETCH = 1.8;

  /** How much quicker the near wall fades than the opening's middle. */
  static final double NEAR_WALL_SQUEEZE = 0.5;

  /** The colour of the rock walls, at their brightest. */
  static final Color WALL_COLOUR = new Color(104, 108, 120);

  /** The colour the walls fade into. */
  static final Color DARK_COLOUR = new Color(6, 6, 10);

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  private static final Map<CaveEntrance, BufferedImage> PAINTED = new ConcurrentHashMap<>();

  private CaveEntrancePainter() {}

  /**
   * Returns the painting of an entrance, painting it the first time.
   *
   * @param entrance the entrance
   * @param surface the block type of the ground it opens in, which picks the rim's colour
   * @return an image exactly covering the entrance's tiles (north at the top), see-through outside
   *     the opening
   */
  static BufferedImage paint(CaveEntrance entrance, BlockType surface) {
    return PAINTED.computeIfAbsent(entrance, e -> paintNew(e, surface));
  }

  /** Returns the colour of the soil showing at the rim of an opening in a block type. */
  static Color rimColour(BlockType surface) {
    switch (surface) {
      case SAND:
        return new Color(186, 158, 98);
      case STONE:
        return new Color(118, 118, 124);
      case SNOW:
        return new Color(196, 206, 220);
      case GRASS:
      case DIRT:
      default:
        return new Color(112, 78, 50);
    }
  }

  private static BufferedImage paintNew(CaveEntrance entrance, BlockType surface) {
    int texelsWide = entrance.widthBlocks() * BLOCK / TEXEL_PX;
    int texelsHigh = entrance.heightBlocks() * BLOCK / TEXEL_PX;
    Random random = new Random(entrance.seed());
    double[] bumps = new double[8];
    for (int i = 0; i < bumps.length; i++) {
      bumps[i] = random.nextDouble() * 2 * Math.PI;
    }
    double centreX = texelsWide / 2.0;
    double centreY = texelsHigh / 2.0;
    // A texel of margin all round, so the opening never touches the edge of its tiles.
    double radiusX = centreX - 1.5;
    double radiusY = centreY - 1.5;
    Color rim = rimColour(surface);
    BufferedImage image =
        new BufferedImage(
            entrance.widthBlocks() * BLOCK,
            entrance.heightBlocks() * BLOCK,
            BufferedImage.TYPE_INT_ARGB);
    for (int tx = 0; tx < texelsWide; tx++) {
      for (int ty = 0; ty < texelsHigh; ty++) {
        double dx = (tx + 0.5 - centreX) / radiusX;
        double dy = (ty + 0.5 - centreY) / radiusY;
        double edge = edgeAt(Math.atan2(dy, dx), bumps);
        double fromMiddle = Math.hypot(dx, dy) / edge;
        if (fromMiddle >= 1) {
          continue;
        }
        double depth = 1 - fromMiddle;
        int colour = colourAt(depth, dy, rim, random.nextDouble());
        fill(image, tx, ty, colour);
      }
    }
    return image;
  }

  /**
   * Returns how far out the opening's edge is in a direction, as a share of the oval's radius:
   * about 1, with a few random bumps so the outline is jagged.
   */
  private static double edgeAt(double angle, double[] bumps) {
    double edge = 0.9;
    for (int i = 0; i < bumps.length / 2; i++) {
      int waves = i + 2;
      edge += (0.09 / (i + 1)) * Math.sin(waves * angle + bumps[i]);
      edge += (0.04 / (i + 1)) * Math.sin(waves * 2 * angle + bumps[i + bumps.length / 2]);
    }
    return edge;
  }

  /**
   * Returns the colour a share {@code depth} of the way in from the edge to the middle: the soil
   * rim, then rock fading into darkness. {@code up} is how far north of the middle it is (negative
   * is north), which stretches the visible far wall and squeezes the near one.
   */
  private static int colourAt(double depth, double up, Color rim, double grain) {
    if (depth < RIM_DEPTH) {
      return shade(rim, 0.85 + 0.15 * grain);
    }
    double stretch = up < 0 ? 1 + (FAR_WALL_STRETCH - 1) * -up : 1 - (1 - NEAR_WALL_SQUEEZE) * up;
    double fade = (depth - RIM_DEPTH) / ((DARK_DEPTH - RIM_DEPTH) * stretch);
    if (fade >= 1) {
      return DARK_COLOUR.getRGB();
    }
    // Into a few steps, so it fades in bands like pixel art, with a little grain in the rock.
    double light = Math.floor((1 - fade) * SHADES) / SHADES;
    light = Math.max(0, light * (0.92 + 0.08 * grain));
    return 0xFF000000
        | mix(DARK_COLOUR.getRed(), WALL_COLOUR.getRed(), light) << 16
        | mix(DARK_COLOUR.getGreen(), WALL_COLOUR.getGreen(), light) << 8
        | mix(DARK_COLOUR.getBlue(), WALL_COLOUR.getBlue(), light);
  }

  private static int shade(Color colour, double light) {
    return 0xFF000000
        | clamp(colour.getRed() * light) << 16
        | clamp(colour.getGreen() * light) << 8
        | clamp(colour.getBlue() * light);
  }

  private static int mix(int from, int to, double amount) {
    return clamp(from + (to - from) * amount);
  }

  private static int clamp(double value) {
    return (int) Math.max(0, Math.min(255, Math.round(value)));
  }

  private static void fill(BufferedImage image, int texelX, int texelY, int colour) {
    for (int x = texelX * TEXEL_PX; x < (texelX + 1) * TEXEL_PX; x++) {
      for (int y = texelY * TEXEL_PX; y < (texelY + 1) * TEXEL_PX; y++) {
        image.setRGB(x, y, colour);
      }
    }
  }
}
