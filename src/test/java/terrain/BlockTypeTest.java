package terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class BlockTypeTest {

  @Test
  void everyBlockTypeHasLowercaseNameAsLabel() {
    for (BlockType type : BlockType.values()) {
      assertEquals(type.name().toLowerCase(Locale.ROOT), type.getLabel());
    }
  }
}
