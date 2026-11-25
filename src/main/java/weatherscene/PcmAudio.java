package weatherscene;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/** Reads 16-bit WAV sounds from the game's resources, and converts them to and from samples. */
final class PcmAudio {

  /**
   * A sound read into memory.
   *
   * @param format how its bytes are laid out
   * @param bytes the sound itself
   */
  record Sound(AudioFormat format, byte[] bytes) {}

  private PcmAudio() {}

  /**
   * Reads a 16-bit WAV file from the game's resources.
   *
   * @param resource the file's path among the resources, such as {@code
   *     /sounds/weather/light-rain.wav}
   * @return the sound, or {@code null} if it is missing, unreadable or not 16-bit
   */
  static Sound read(String resource) {
    try (InputStream file = PcmAudio.class.getResourceAsStream(resource)) {
      if (file == null) {
        System.err.println("Sound not found: " + resource);
        return null;
      }
      try (AudioInputStream audio =
          AudioSystem.getAudioInputStream(new BufferedInputStream(file))) {
        AudioFormat format = audio.getFormat();
        if (format.getSampleSizeInBits() != 16
            || format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
          System.err.println("Sound must be 16-bit PCM: " + resource);
          return null;
        }
        return new Sound(format, audio.readAllBytes());
      }
    } catch (IOException | UnsupportedAudioFileException e) {
      System.err.println("Could not read sound " + resource + ": " + e);
      return null;
    }
  }

  /**
   * Converts a sound's bytes to samples.
   *
   * @param bytes 16-bit samples, two bytes each
   * @param bigEndian whether each sample's high byte comes first
   * @return the samples
   */
  static short[] toSamples(byte[] bytes, boolean bigEndian) {
    short[] samples = new short[bytes.length / 2];
    for (int i = 0; i < samples.length; i++) {
      int first = bytes[2 * i] & 0xFF;
      int second = bytes[2 * i + 1] & 0xFF;
      samples[i] = (short) (bigEndian ? first << 8 | second : second << 8 | first);
    }
    return samples;
  }

  /**
   * Converts samples back to a sound's bytes.
   *
   * @param samples the samples
   * @param bigEndian whether each sample's high byte should come first
   * @return the bytes, two per sample
   */
  static byte[] toBytes(short[] samples, boolean bigEndian) {
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
