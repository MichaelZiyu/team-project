package game.gameOutput;

import java.util.List;

/**
 * Plays the game's sound, as the renderer works it out from each frame: which looping sounds should
 * be heard and how loudly, and which sounds to play once, now.
 */
public interface GameAudioBoundary {

  /** Plays nothing; for a game with no sound. */
  GameAudioBoundary SILENT = (loops, now) -> {};

  /**
   * A sound that plays on and on.
   *
   * @param name the sound, named by its file under the sound folder without the ending
   * @param volume how loud, from 0 (silent) to 1 (full)
   */
  record Loop(String name, double volume) {}

  /**
   * A sound to play once, now.
   *
   * @param name the sound, named by its file under the sound folder without the ending
   * @param volume how loud, from 0 (silent) to 1 (full)
   */
  record OneShot(String name, double volume) {}

  /**
   * Brings the sound up to date: every loop listed plays at its volume, every loop not listed
   * stops, and each one-shot starts playing. Called once each frame, on the renderer's thread.
   *
   * @param loops the loops to be heard now; each name at most once
   * @param now the sounds to start playing now, each heard once
   */
  void update(List<Loop> loops, List<OneShot> now);
}
