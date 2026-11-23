package menu;

import game.gameOutput.GameAudioBoundary;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Plays the game's sound through Java's built-in sound, as the renderer asks.
 *
 * <p>Sounds are 16-bit WAV files under {@link #SOUND_FOLDER} among the game's resources, named
 * without their ending: {@code "weather/light-rain"} is {@code sounds/weather/light-rain.wav}.
 * Each is read once, the first time it is needed, and kept.
 *
 * <p>A looping sound plays on its own clip for as long as it is asked for, at the volume asked;
 * when it is no longer asked for, it stops. Recordings rarely loop cleanly, so the last {@link
 * #LOOP_BLEND_SECONDS} of a looping sound are blended into its start ({@link #seamlessLoop}) and
 * the loop has no seam. A sound played once gets a clip of its own, so it can overlap others; at
 * most {@link #MAX_ONE_SHOTS} play together, the oldest being cut off to make room.
 *
 * <p>A missing or unreadable sound, or a computer with no sound, is reported once and then simply
 * not heard: sound never stops the game.
 */
public final class GameAudio implements GameAudioBoundary {

  /** Where sounds are among the game's resources. */
  static final String SOUND_FOLDER = "/sounds/";

  /** How much of the end of a looping sound is blended into its start, in seconds. */
  static final double LOOP_BLEND_SECONDS = 0.5;

  /** The most sounds played once that play together. */
  static final int MAX_ONE_SHOTS = 8;

  /** The quietest volume set, in decibels; quieter than that is stopped instead. */
  private static final float MIN_GAIN_DB = -60;

  /** A sound read into memory. */
  private record Sound(AudioFormat format, byte[] bytes) {}

  /** Each sound read so far, or empty if it could not be read. */
  private final Map<String, Optional<Sound>> sounds = new HashMap<>();

  /** A clip for each looping sound used so far, or empty if it cannot be played. */
  private final Map<String, Optional<Clip>> loops = new HashMap<>();

  /** The looping sounds playing right now. */
  private final Set<String> looping = new HashSet<>();

  /** The sounds played once that are still playing, oldest first. */
  private final Deque<Clip> oneShots = new ArrayDeque<>();

  @Override
  public synchronized void update(List<Loop> wanted, List<OneShot> now) {
    Set<String> heard = new HashSet<>();
    for (Loop loop : wanted) {
      heard.add(loop.name());
      Optional<Clip> clip = loops.computeIfAbsent(loop.name(), this::openLoop);
      clip.ifPresent(c -> setLoop(loop.name(), c, loop.volume()));
    }
    for (String name : Set.copyOf(looping)) {
      if (!heard.contains(name)) {
        loops.get(name).ifPresent(Clip::stop);
        looping.remove(name);
      }
    }
    for (OneShot shot : now) {
      playOnce(shot);
    }
  }

  /** Stops every sound for good and frees what they hold. */
  public synchronized void close() {
    for (Optional<Clip> clip : loops.values()) {
      clip.ifPresent(Clip::close);
    }
    loops.clear();
    looping.clear();
    for (Clip clip : oneShots) {
      clip.close();
    }
    oneShots.clear();
  }

  /**
   * Returns the decibels to set a clip to for a volume from 0 to 1: sound is heard in decibels,
   * twenty times the base-10 logarithm of the volume, down to a floor.
   *
   * @param volume from 0 (silent) to 1 (full)
   * @return the decibels, from {@link #MIN_GAIN_DB} to 0
   */
  static float decibels(double volume) {
    if (volume <= 0) {
      return MIN_GAIN_DB;
    }
    return (float) Math.max(MIN_GAIN_DB, 20 * Math.log10(Math.min(1, volume)));
  }

  /**
   * Returns a sound shortened so it loops without a seam: its last {@code blendFrames} frames are
   * faded out over its first ones as they fade in, then left off the end. On repeat, the end runs
   * straight on into the start, because the start now begins with what used to follow the end.
   *
   * @param samples the sound's samples, channels interleaved
   * @param channels how many channels (2 for stereo)
   * @param blendFrames how many frames to blend; at most half the sound
   * @return the shortened, loopable samples
   * @throws IllegalArgumentException if the sound is too short to blend that much
   */
  static short[] seamlessLoop(short[] samples, int channels, int blendFrames) {
    int frames = samples.length / channels;
    if (blendFrames < 0 || 2 * blendFrames > frames) {
      throw new IllegalArgumentException(
          "cannot blend " + blendFrames + " frames of a " + frames + "-frame sound");
    }
    int loopFrames = frames - blendFrames;
    short[] loop = new short[loopFrames * channels];
    System.arraycopy(samples, 0, loop, 0, loop.length);
    for (int frame = 0; frame < blendFrames; frame++) {
      double in = (frame + 0.5) / blendFrames;
      // Equal power, so the blend is neither louder nor quieter than either part.
      double startWeight = Math.sin(in * Math.PI / 2);
      double endWeight = Math.cos(in * Math.PI / 2);
      for (int channel = 0; channel < channels; channel++) {
        int at = frame * channels + channel;
        int fromEnd = (loopFrames + frame) * channels + channel;
        double mixed = samples[at] * startWeight + samples[fromEnd] * endWeight;
        loop[at] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, mixed));
      }
    }
    return loop;
  }

  /** Sets a looping sound's volume, starting or stopping it as needed. */
  private void setLoop(String name, Clip clip, double volume) {
    if (volume <= 0) {
      if (looping.remove(name)) {
        clip.stop();
      }
      return;
    }
    setVolume(clip, volume);
    if (looping.add(name)) {
      clip.loop(Clip.LOOP_CONTINUOUSLY);
    }
  }

  /** Starts a sound playing once, on a clip of its own that is freed when it ends. */
  private void playOnce(OneShot shot) {
    Optional<Sound> sound = sounds.computeIfAbsent(shot.name(), GameAudio::read);
    if (sound.isEmpty() || shot.volume() <= 0) {
      return;
    }
    try {
      Clip clip = AudioSystem.getClip();
      clip.open(sound.get().format(), sound.get().bytes(), 0, sound.get().bytes().length);
      clip.addLineListener(
          event -> {
            if (event.getType() == LineEvent.Type.STOP) {
              finished(clip);
            }
          });
      while (oneShots.size() >= MAX_ONE_SHOTS) {
        oneShots.removeFirst().close();
      }
      oneShots.addLast(clip);
      setVolume(clip, shot.volume());
      clip.start();
    } catch (LineUnavailableException | IllegalArgumentException e) {
      System.err.println("Could not play sound " + shot.name() + ": " + e);
    }
  }

  private synchronized void finished(Clip clip) {
    oneShots.remove(clip);
    clip.close();
  }

  /** Opens a clip ready to loop a sound without a seam, or nothing if it cannot be played. */
  private Optional<Clip> openLoop(String name) {
    Optional<Sound> sound = sounds.computeIfAbsent(name, GameAudio::read);
    if (sound.isEmpty()) {
      return Optional.empty();
    }
    AudioFormat format = sound.get().format();
    try {
      int blendFrames = (int) Math.round(LOOP_BLEND_SECONDS * format.getFrameRate());
      short[] samples = toSamples(sound.get().bytes(), format.isBigEndian());
      byte[] loop =
          toBytes(seamlessLoop(samples, format.getChannels(), blendFrames), format.isBigEndian());
      Clip clip = AudioSystem.getClip();
      clip.open(format, loop, 0, loop.length);
      return Optional.of(clip);
    } catch (LineUnavailableException | IllegalArgumentException e) {
      System.err.println("Could not loop sound " + name + ": " + e);
      return Optional.empty();
    }
  }

  private static void setVolume(Clip clip, double volume) {
    if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
      FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
      gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels(volume))));
    }
  }

  /** Reads a 16-bit WAV sound from the game's resources, or nothing if it cannot. */
  private static Optional<Sound> read(String name) {
    String resource = SOUND_FOLDER + name + ".wav";
    try (InputStream file = GameAudio.class.getResourceAsStream(resource)) {
      if (file == null) {
        System.err.println("Sound not found: " + resource);
        return Optional.empty();
      }
      try (AudioInputStream audio =
          AudioSystem.getAudioInputStream(new BufferedInputStream(file))) {
        AudioFormat format = audio.getFormat();
        if (format.getSampleSizeInBits() != 16
            || format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
          System.err.println("Sound must be 16-bit PCM: " + resource);
          return Optional.empty();
        }
        return Optional.of(new Sound(format, audio.readAllBytes()));
      }
    } catch (IOException | UnsupportedAudioFileException e) {
      System.err.println("Could not read sound " + resource + ": " + e);
      return Optional.empty();
    }
  }

  private static short[] toSamples(byte[] bytes, boolean bigEndian) {
    short[] samples = new short[bytes.length / 2];
    for (int i = 0; i < samples.length; i++) {
      int first = bytes[2 * i] & 0xFF;
      int second = bytes[2 * i + 1] & 0xFF;
      samples[i] = (short) (bigEndian ? first << 8 | second : second << 8 | first);
    }
    return samples;
  }

  private static byte[] toBytes(short[] samples, boolean bigEndian) {
    ByteArrayOutputStream out = new ByteArrayOutputStream(samples.length * 2);
    for (short sample : samples) {
      int high = (sample >> 8) & 0xFF;
      int low = sample & 0xFF;
      out.write(bigEndian ? high : low);
      out.write(bigEndian ? low : high);
    }
    return out.toByteArray();
  }
}
