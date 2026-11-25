package weatherscene;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;

/**
 * Plays thunder through Java's built-in sound. Each rumble gets its own clip, so a new one can
 * start while earlier ones are still fading away; at most {@link #MAX_AT_ONCE} play together,
 * the oldest being cut off to make room. Each clip is freed when it finishes.
 */
final class ClipThunderPlayer implements ThunderPlayer {

  /** The most rumbles that play at once. */
  static final int MAX_AT_ONCE = 4;

  private final List<PcmAudio.Sound> sounds = new ArrayList<>();
  private final Deque<Clip> playing = new ArrayDeque<>();

  /**
   * Reads the thunder sounds from the game's resources; any that are missing are left out.
   *
   * @param resources each sound's path among the resources
   */
  ClipThunderPlayer(String... resources) {
    for (String resource : resources) {
      PcmAudio.Sound sound = PcmAudio.read(resource);
      if (sound != null) {
        sounds.add(sound);
      }
    }
  }

  @Override
  public int soundCount() {
    return sounds.size();
  }

  @Override
  public synchronized void play(int sound) {
    PcmAudio.Sound chosen = sounds.get(sound);
    try {
      Clip clip = AudioSystem.getClip();
      clip.open(chosen.format(), chosen.bytes(), 0, chosen.bytes().length);
      clip.addLineListener(
          event -> {
            if (event.getType() == LineEvent.Type.STOP) {
              finished(clip);
            }
          });
      while (playing.size() >= MAX_AT_ONCE) {
        playing.removeFirst().close();
      }
      playing.addLast(clip);
      clip.start();
    } catch (LineUnavailableException | IllegalArgumentException e) {
      System.err.println("Could not play thunder: " + e);
    }
  }

  @Override
  public synchronized void close() {
    for (Clip clip : playing) {
      clip.close();
    }
    playing.clear();
  }

  private synchronized void finished(Clip clip) {
    playing.remove(clip);
    clip.close();
  }
}
