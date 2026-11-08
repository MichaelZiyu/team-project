package terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TerrainCoordinatesTest {

  @Test
  void pixelToBlockRoundsPositivePixelsDown() {
    assertEquals(0, TerrainCoordinates.pixelToBlock(0));
    assertEquals(0, TerrainCoordinates.pixelToBlock(63.9));
    assertEquals(1, TerrainCoordinates.pixelToBlock(64));
  }

  @Test
  void pixelToBlockRoundsNegativePixelsDown() {
    assertEquals(-1, TerrainCoordinates.pixelToBlock(-0.5));
    assertEquals(-1, TerrainCoordinates.pixelToBlock(-64));
    assertEquals(-2, TerrainCoordinates.pixelToBlock(-64.1));
  }

  @Test
  void blockToPixelGivesBlockStart() {
    assertEquals(0, TerrainCoordinates.blockToPixel(0));
    assertEquals(128, TerrainCoordinates.blockToPixel(2));
    assertEquals(-64, TerrainCoordinates.blockToPixel(-1));
  }

  @Test
  void blockToChunkHandlesChunkEdges() {
    assertEquals(0, TerrainCoordinates.blockToChunk(0));
    assertEquals(0, TerrainCoordinates.blockToChunk(15));
    assertEquals(1, TerrainCoordinates.blockToChunk(16));
  }

  @Test
  void blockToChunkPutsNegativeBlocksInNegativeChunks() {
    assertEquals(-1, TerrainCoordinates.blockToChunk(-1));
    assertEquals(-1, TerrainCoordinates.blockToChunk(-16));
    assertEquals(-2, TerrainCoordinates.blockToChunk(-17));
  }

  @Test
  void blockToLocalStaysInsideChunk() {
    assertEquals(0, TerrainCoordinates.blockToLocal(0));
    assertEquals(15, TerrainCoordinates.blockToLocal(15));
    assertEquals(0, TerrainCoordinates.blockToLocal(16));
    assertEquals(15, TerrainCoordinates.blockToLocal(-1));
    assertEquals(0, TerrainCoordinates.blockToLocal(-16));
  }
}
