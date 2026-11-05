package terrainscene;

import java.awt.Color;

/**
 * The colour washed over the whole screen at each time of day: nothing at midday, a warm orange
 * glow at dawn and dusk, and a dim moonlit blue at night.
 *
 * <p>The colour is see-through; its opacity says how strong the wash is. Between the key times
 * below, the colour blends smoothly from one to the next.
 */
public final class SkyTint {

  /** The night wash: deep blue at about 45%, dim but still easy to see by. */
  static final Color NIGHT = new Color(14, 22, 64, 115);

  /** The sunrise glow: warm orange at about 20%. */
  static final Color DAWN = new Color(255, 150, 80, 50);

  /** The sunset glow: a redder orange at about 25%. */
  static final Color DUSK = new Color(255, 120, 70, 65);

  /** No wash at all, in the middle of the day. */
  static final Color DAY = new Color(255, 230, 200, 0);

  /** Times of day (fractions of a day) at which the wash is exactly one of the colours. */
  private static final double[] KEY_TIMES = {0, 0.20, 0.25, 0.32, 0.68, 0.75, 0.80, 1};

  /** The colour at each key time. */
  private static final Color[] KEY_COLOURS = {NIGHT, NIGHT, DAWN, DAY, DAY, DUSK, NIGHT, NIGHT};

  private SkyTint() {}

  /**
   * Returns the wash for a time of day.
   *
   * @param timeOfDay a fraction of a day: 0 is midnight, 0.5 is noon
   * @return a see-through colour; fully transparent in the middle of the day
   */
  public static Color at(double timeOfDay) {
    double time = timeOfDay - Math.floor(timeOfDay);
    int next = 1;
    while (KEY_TIMES[next] < time) {
      next++;
    }
    double start = KEY_TIMES[next - 1];
    double fraction = (time - start) / (KEY_TIMES[next] - start);
    return blend(KEY_COLOURS[next - 1], KEY_COLOURS[next], fraction);
  }

  /** Returns the colour {@code fraction} of the way from {@code from} to {@code to}. */
  private static Color blend(Color from, Color to, double fraction) {
    return new Color(
        mix(from.getRed(), to.getRed(), fraction),
        mix(from.getGreen(), to.getGreen(), fraction),
        mix(from.getBlue(), to.getBlue(), fraction),
        mix(from.getAlpha(), to.getAlpha(), fraction));
  }

  private static int mix(int from, int to, double fraction) {
    return (int) Math.round(from + (to - from) * fraction);
  }
}
