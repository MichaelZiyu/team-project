package terrainscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.gameModel.Renderable;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import terrain.BlockPos;
import terrain.CaveTerrain;
import terrain.TerrainCoordinates;

class CaveDarknessTest {

  private static final int BLOCK = TerrainCoordinates.BLOCK_SIZE_PX;

  private final CaveTerrain cave = new CaveTerrain(1234L);

  @Test
  void itIsLitNearThePlayerAndGloomyFarAway() {
    assertEquals(0, CaveDarkness.darknessAt(0), 1e-9);
    assertEquals(0, CaveDarkness.darknessAt(CaveDarkness.LIT_RADIUS_PX), 1e-9);
    assertEquals(1, CaveDarkness.darknessAt(CaveDarkness.DARK_RADIUS_PX), 1e-9);
    double previous = 0;
    for (double distance = 0; distance < 600; distance += 10) {
      double dark = CaveDarkness.darknessAt(distance);
      assertTrue(dark >= previous, "darker further away");
      previous = dark;
    }
  }

  @Test
  void theLayerCoversTheScreenAroundThePlayer() {
    CaveDarkness darkness = new CaveDarkness(cave, 800, 600);
    darkness.update(1000.7, -300.2);
    List<Renderable> contents = new ArrayList<>();
    darkness.forEach(contents::add);
    assertEquals(1, contents.size());
    Renderable layer = contents.get(0);
    // The game camera's rule, for a player at whole pixel (1000, -300).
    int screenX = layer.getX() - 1000 + 400 - layer.getWidth() / 2;
    int screenY = 300 - (layer.getY() - (-300)) - layer.getHeight();
    assertEquals(-CaveDarkness.BORDER_PX, screenX);
    assertEquals(-CaveDarkness.BORDER_PX, screenY);
  }

  @Test
  void rockTopsAreDarkEvenNextToThePlayer() {
    BlockPos spawn = cave.spawnPoint();
    // Find solid rock near where the player arrives, and stand beside it.
    int rockX = spawn.blockX();
    int rockY = spawn.blockY();
    while (cave.isOpen(rockX, rockY)) {
      rockX++;
    }
    double playerX = (rockX - 1) * BLOCK + BLOCK / 2.0;
    double playerY = rockY * BLOCK;
    CaveDarkness darkness = new CaveDarkness(cave, 800, 600);
    darkness.update(playerX, playerY);
    BufferedImage image = darkness.image();
    int lift = CaveTerrain.WALL_HEIGHT * TileRenderable.STEP_HEIGHT_PX;
    int rockTopX = rockX * BLOCK - (int) playerX + image.getWidth() / 2 + BLOCK / 2;
    int rockTopY = image.getHeight() / 2 - ((rockY + 1) * BLOCK + lift - (int) playerY) + BLOCK / 2;
    int floorX = image.getWidth() / 2;
    int floorY = image.getHeight() / 2 - CaveDarkness.LIGHT_ABOVE_FEET_PX;
    int rockAlpha = image.getRGB(rockTopX, rockTopY) >>> 24;
    int floorAlpha = image.getRGB(floorX, floorY) >>> 24;
    assertTrue(rockAlpha > 180, "rock top is dark: " + rockAlpha);
    assertTrue(floorAlpha < 80, "the floor by the player is lit: " + floorAlpha);
  }
}
