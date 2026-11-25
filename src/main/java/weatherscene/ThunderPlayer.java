package weatherscene;

/** Plays thunder sounds, each as soon as it is asked for, overlapping any still rumbling. */
interface ThunderPlayer {

  /**
   * Returns how many different thunder sounds there are to choose from.
   *
   * @return the number of sounds; 0 if there are none (or no sound device)
   */
  int soundCount();

  /**
   * Starts one of the sounds playing.
   *
   * @param sound which one, from 0 to {@link #soundCount()} - 1
   */
  void play(int sound);

  /** Stops every sound for good and frees what they hold. */
  void close();
}
