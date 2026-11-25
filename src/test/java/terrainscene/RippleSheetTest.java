package terrainscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
import javax.swing.JLabel;
import org.junit.jupiter.api.Test;

class RippleSheetTest {

  private static final CompletableFuture<WaterRipples> READY =
      CompletableFuture.completedFuture(WaterRipples.create(WaterRipples.SEED));

  @Test
  void sheetCoversTheScreenWithAMargin() {
    RippleSheet sheet = new RippleSheet(800, 600, READY);
    sheet.moveTo(1000.4, -250.6);
    assertEquals(800 + 2 * RippleSheet.MARGIN_PX, sheet.getWidth());
    assertEquals(600 + 2 * RippleSheet.MARGIN_PX, sheet.getHeight());
    assertEquals(1000, sheet.getX());
    assertEquals(-251 - sheet.getHeight() / 2, sheet.getY());
  }

  @Test
  void swingCanPaintTheSheet() {
    RippleSheet sheet = new RippleSheet(300, 200, READY);
    assertTrue(sheet.isReady());
    sheet.moveTo(0, 0);
    JLabel label = sheet.getTexture().getContent();
    label.setSize(sheet.getWidth(), sheet.getHeight());
    BufferedImage canvas =
        new BufferedImage(sheet.getWidth(), sheet.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = canvas.createGraphics();
    try {
      // Painted the way Swing paints it, through the label.
      label.paint(g);
    } finally {
      g.dispose();
    }
    int drawn = 0;
    for (int y = 0; y < canvas.getHeight(); y++) {
      for (int x = 0; x < canvas.getWidth(); x++) {
        if (canvas.getRGB(x, y) >>> 24 != 0) {
          drawn++;
        }
      }
    }
    assertTrue(drawn > 0, "no ripples were drawn");
  }

  @Test
  void ripplesStayFixedToTheWorldAsTheCameraMoves() {
    RippleSheet sheet = new RippleSheet(300, 200, READY);
    BufferedImage before = paint(sheet, 0, 0);
    BufferedImage after = paint(sheet, 10, -6);
    // Moving the camera 10 east and 6 south slides the world 10 left and 6 up on the sheet.
    for (int y = 20; y < 100; y++) {
      for (int x = 20; x < 200; x++) {
        assertEquals(before.getRGB(x + 10, y + 6), after.getRGB(x, y), "at " + x + "," + y);
      }
    }
  }

  private static BufferedImage paint(RippleSheet sheet, double cameraX, double cameraY) {
    sheet.moveTo(cameraX, cameraY);
    BufferedImage canvas =
        new BufferedImage(sheet.getWidth(), sheet.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = canvas.createGraphics();
    try {
      sheet.paintSheet(g);
    } finally {
      g.dispose();
    }
    return canvas;
  }

  @Test
  void brighterRipplesAreMoreOpaque() {
    RippleSheet sheet = new RippleSheet(300, 200, READY);
    BufferedImage usual = paint(sheet, 0, 0);
    sheet.setBrightness(RippleSheet.MAX_BRIGHTNESS);
    BufferedImage bright = paint(sheet, 0, 0);
    int usualTotal = 0;
    int brightTotal = 0;
    for (int y = 0; y < usual.getHeight(); y++) {
      for (int x = 0; x < usual.getWidth(); x++) {
        usualTotal += usual.getRGB(x, y) >>> 24;
        brightTotal += bright.getRGB(x, y) >>> 24;
      }
    }
    assertTrue(brightTotal > 2 * usualTotal, "glare should be much stronger");
    assertThrows(IllegalArgumentException.class, () -> sheet.setBrightness(0.5));
  }

  @Test
  void aSheetCanCoverAFixedArea() {
    RippleSheet sheet = new RippleSheet(300, 200, READY);
    sheet.cover(-640, 1280, 900, 700);
    assertEquals(-640 + 450, sheet.getX());
    assertEquals(1280, sheet.getY());
    assertEquals(900, sheet.getWidth());
    assertEquals(700, sheet.getHeight());
    assertEquals(900, sheet.getTexture().getWidth());
  }
}
