package weatherscene;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;

/**
 * A looping sound played from a WAV file, through Java's built-in sound.
 *
 * <p>Recordings rarely loop cleanly: the end does not lead into the start, so each repeat clicks
 * or jumps. When loaded, the last {@link #LOOP_BLEND_SECONDS} of the sound are blended into its
 * first moments ({@link #seamlessLoop}), and the end trimmed off, so the loop runs on without a
 * seam.
 */
final class ClipTrack implements SoundTrack {

  /** How much of the end of the sound is blended into its start, in seconds. */
  static final double LOOP_BLEND_SECONDS = 0.5;

  /** The quietest volume set, in decibels; anything quieter is stopped instead. */
  private static final float MIN_GAIN_DB = -60;

  private final Clip clip;
  private final FloatControl gain;

  private ClipTrack(Clip clip) {
    this.clip = clip;
    this.gain =
        clip.isControlSupported(FloatControl.Type.MASTER_GAIN)
            ? (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN)
            : null;
  }

  /**
   * Loads a 16-bit WAV file from the game's resources, ready to loop.
   *
   * @param resource the file's path among the resources, such as {@code
   *     /sounds/weather/light-rain.wav}
   * @return the track; {@link SoundTrack#SILENT} if the file is missing or unreadable, or there is
   *     no sound device
   */
  static SoundTrack load(String resource) {
    PcmAudio.Sound sound = PcmAudio.read(resource);
    if (sound == null) {
      return SoundTrack.SILENT;
    }
    AudioFormat format = sound.format();
    try {
      int blendFrames = (int) Math.round(LOOP_BLEND_SECONDS * format.getFrameRate());
      short[] samples = PcmAudio.toSamples(sound.bytes(), format.isBigEndian());
      byte[] loop =
          PcmAudio.toBytes(
              seamlessLoop(samples, format.getChannels(), blendFrames), format.isBigEndian());
      Clip clip = AudioSystem.getClip();
      clip.open(format, loop, 0, loop.length);
      return new ClipTrack(clip);
    } catch (LineUnavailableException | IllegalArgumentException e) {
      System.err.println("Could not play sound " + resource + ": " + e);
      return SoundTrack.SILENT;
    }
  }

  @Override
  public void setLevel(double level) {
    if (level <= 0) {
      if (clip.isRunning()) {
        clip.stop();
      }
      return;
    }
    if (gain != null) {
      // Loudness is heard in decibels: 20 log10 of the level, kept within what the line allows.
      float decibels = (float) (20 * Math.log10(Math.min(1, level)));
      decibels = Math.max(Math.max(MIN_GAIN_DB, gain.getMinimum()), decibels);
      gain.setValue(Math.min(gain.getMaximum(), decibels));
    }
    if (!clip.isRunning()) {
      clip.loop(Clip.LOOP_CONTINUOUSLY);
    }
  }

  @Override
  public void close() {
    clip.close();
  }

  /**
   * Returns a sound shortened so it loops without a seam: its last {@code blendFrames} frames are
   * faded out over its first ones as they fade in, and then left off the end. Playing the result
   * on repeat, the end runs straight on into the start, because the start now begins with what
   * used to follow the end.
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
}
