package weatherscene;

/** One looping sound whose volume can be turned up and down, such as falling rain. */
interface SoundTrack {

  /**
   * Sets how loud the sound plays. It starts looping when raised above 0, and stops at 0.
   *
   * @param level from 0 (silent and stopped) to 1 (full volume)
   */
  void setLevel(double level);

  /** Stops the sound for good and frees what it holds. */
  void close();

  /** A track that makes no sound, for when there is no sound device or the file is missing. */
  SoundTrack SILENT =
      new SoundTrack() {
        @Override
        public void setLevel(double level) {}

        @Override
        public void close() {}
      };
}
