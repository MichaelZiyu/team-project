package terrainscene;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import terrain.BlockType;

class FaceRenderableTest {

  @Test
  void lowerStepsUseABlockTypesOwnFaceWhenItHasOne() {
    for (BlockType type : BlockType.values()) {
      String own = FaceRenderable.TEXTURE_FOLDER + "/" + type.getLabel();
      boolean hasOwn = Files.isDirectory(TerrainImages.TEXTURES_ROOT.resolve(own));
      String expected = hasOwn ? own : FaceRenderable.TEXTURE_FOLDER;
      assertEquals(expected, FaceRenderable.lowerStepFolder(type), type.toString());
      FaceRenderable lower = new FaceRenderable(0, 0, type, 0, false);
      assertEquals(expected, lower.imageFolder());
    }
  }

  @Test
  void theTopStepAlwaysUsesItsBlockTypesOwnImage() {
    FaceRenderable top = new FaceRenderable(0, 0, BlockType.GRASS, 1, true);
    assertEquals(FaceRenderable.TOP_STEP_TEXTURE_FOLDER + "grass", top.imageFolder());
  }
}
