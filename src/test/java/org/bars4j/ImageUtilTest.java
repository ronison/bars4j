package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

import org.bars4j.util.ImageUtil;
import org.junit.jupiter.api.Test;

/**
 * Phase 3: ImageUtil is now backed by the standard ImageIO API (no vendored encoders). It also
 * writes pixel-density metadata (PNG pHYs / JPEG JFIF) and flattens alpha over white for JPEG.
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
            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(bytes));
            assertNotNull(decoded, "decoded " + format);
            // PNG/GIF are lossless; JPEG is lossy so only check dimensions.
            assertTrue(decoded.getWidth() == 10 && decoded.getHeight() == 10);
        }
    }

    @Test
    void rejectsUnsupportedFormat() {
        assertThrows(IOException.class, () -> ImageUtil.encode(sample(), "bmp"));
    }

    @Test
    void rejectsNonPositiveDpi() {
        assertThrows(IOException.class, () -> ImageUtil.encode(sample(), ImageUtil.PNG, 0, 96));
        assertThrows(IOException.class, () -> ImageUtil.encode(sample(), ImageUtil.PNG, 96, -1));
    }

    @Test
    void pngCarriesPhysDensity() throws Exception {
        byte[] png = ImageUtil.encode(sample(), ImageUtil.PNG, 300, 600);
        IIOMetadataNode dimension = dimensionNode(png, "png");
        double xDpi = 25.4 / Double.parseDouble(value(dimension, "HorizontalPixelSize"));
        double yDpi = 25.4 / Double.parseDouble(value(dimension, "VerticalPixelSize"));
        assertEquals(300, Math.round(xDpi), "pHYs X density");
        assertEquals(600, Math.round(yDpi), "pHYs Y density");
    }

    @Test
    void jpegCarriesJfifDensityAndFlattensAlpha() throws Exception {
        // TYPE_INT_ARGB has alpha: the JPEG must composite transparent pixels over white.
        BufferedImage argb = new BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB);
        argb.setRGB(0, 0, 0xFF000000); // left half opaque black, right half transparent
        byte[] jpeg = ImageUtil.encode(argb, ImageUtil.JPEG, 150, 150);
        IIOMetadataNode jfifRoot = (IIOMetadataNode) metadata(jpeg, "jpeg")
                .getAsTree("javax_imageio_jpeg_image_1.0");
        IIOMetadataNode app0 = (IIOMetadataNode) jfifRoot
                .getElementsByTagName("app0JFIF").item(0);
        assertEquals("150", app0.getAttribute("Xdensity"), "JFIF Xdensity");
        assertEquals("150", app0.getAttribute("Ydensity"), "JFIF Ydensity");
        assertEquals("1", app0.getAttribute("resUnits"), "JFIF resUnits (dots per inch)");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(jpeg));
        int alphaArea = decoded.getRGB(15, 5) & 0x00FFFFFF;
        assertTrue(alphaArea > 0xF0F0F0, "transparent pixels composited over white, got "
                + Integer.toHexString(alphaArea));
    }

    private static IIOMetadata metadata(byte[] bytes, String format) throws IOException {
        var reader = ImageIO.getImageReadersByFormatName(format).next();
        reader.setInput(ImageIO.createImageInputStream(new ByteArrayInputStream(bytes)));
        return reader.getImageMetadata(0);
    }

    private static IIOMetadataNode dimensionNode(byte[] png, String format) throws IOException {
        IIOMetadataNode root = (IIOMetadataNode) metadata(png, format).getAsTree("javax_imageio_1.0");
        return (IIOMetadataNode) root.getElementsByTagName("Dimension").item(0);
    }

    private static String value(IIOMetadataNode parent, String child) {
        IIOMetadataNode node = (IIOMetadataNode) parent.getElementsByTagName(child).item(0);
        assertNotNull(node, child + " present");
        return node.getAttribute("value");
    }
}
