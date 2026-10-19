package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.bars4j.util.ImageUtil;
import org.junit.jupiter.api.Test;

/**
 * Phase 3: ImageUtil is now backed by the standard ImageIO API (no vendored encoders).
 */
class ImageUtilTest {

    private static BufferedImage sample() {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        image.setRGB(5, 5, 0x000000);
        return image;
    }

    @Test
    void encodesPngJpegAndGif() throws Exception {
        for (String format : new String[]{ImageUtil.PNG, ImageUtil.JPEG, ImageUtil.GIF}) {
            byte[] bytes = ImageUtil.encode(sample(), format);
            assertTrue(bytes.length > 0, format + " should produce bytes");
            BufferedImage decoded = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            assertNotNull(decoded, "decoded " + format);
            // PNG/GIF are lossless; JPEG is lossy so only check dimensions.
            assertTrue(decoded.getWidth() == 10 && decoded.getHeight() == 10);
        }
    }

    @Test
    void rejectsUnsupportedFormat() {
        assertThrows(IOException.class, () -> ImageUtil.encode(sample(), "bmp"));
    }
}
