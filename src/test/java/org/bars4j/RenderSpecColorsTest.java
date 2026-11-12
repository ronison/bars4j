package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.bars4j.api.BarcodeGenerator;
import org.bars4j.render.RenderSpec;
import org.junit.jupiter.api.Test;

/**
 * The linear generators honour the {@link RenderSpec} foreground/background colours in both
 * raster and SVG output.
 */
class RenderSpecColorsTest {

    private static RenderSpec spec(Color foreground, Color background, RenderSpec base) {
        return new RenderSpec(base.barWidth(), base.barHeight(), base.wideRatio(), base.moduleSize(),
                foreground, background);
    }

    private static final RenderSpec RED_BLUE = spec(new Color(0xE5, 0x39, 0x35),
            new Color(0x0D, 0x47, 0xA1), RenderSpec.linearDefaults());

    @Test
    void rasterUsesSpecColors() throws Exception {
        BarcodeGenerator generator = BarcodeGenerator.of("CODE128");
        BufferedImage image = generator.with(RED_BLUE).toImage("BARS4J");
        assertColorPresent(image, 0xFFE53935, "red foreground");
        assertColorPresent(image, 0xFF0D47A1, "blue background");
        assertColorAbsent(image, 0xFF000000, "no default black");
        assertColorAbsent(image, 0xFFFFFFFF, "no default white");
    }

    @Test
    void svgUsesSpecColors() throws Exception {
        BarcodeGenerator generator = BarcodeGenerator.of("CODE128");
        String svg = generator.with(RED_BLUE).toSvg("BARS4J");
        assertTrue(svg.contains("fill=\"#e53935\""), "foreground hex in SVG path");
        assertTrue(svg.contains("fill=\"#0d47a1\""), "background hex in SVG path");
    }

    @Test
    void textPaintingUsesSpecColors() throws Exception {
        BarcodeGenerator generator = BarcodeGenerator.of("CODE39");
        RenderSpec green = spec(new Color(0x2E, 0x7D, 0x32), Color.WHITE, RenderSpec.linearDefaults());
        BufferedImage image = generator.with(green).toImage("B4J");
        assertColorPresent(image, 0xFF2E7D32, "green text/foreground");
    }

    @Test
    void heightCodedFormatHonorsSpec() throws Exception {
        BarcodeGenerator generator = BarcodeGenerator.of("POSTNET");
        BufferedImage image = generator.with(RED_BLUE).toImage("805365961");
        assertColorPresent(image, 0xFFE53935, "red foreground");
        assertColorPresent(image, 0xFF0D47A1, "blue background");
    }

    @Test
    void ean13SvgHasNoLeadingChevron() throws Exception {
        // The legacy EAN-13 SVG used to append a ">" placeholder in the check-digit slot.
        String svg = BarcodeGenerator.of("EAN13").toSvg("789100031550");
        assertTrue(svg.contains(">315507</text>"), "check digit in the third text group");
        assertEquals(-1, svg.indexOf("textLength=\"8\">>"), "no chevron placeholder");
    }

    private static void assertColorPresent(BufferedImage image, int rgb, String label) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) == rgb) {
                    return;
                }
            }
        }
        throw new AssertionError(label + " not found in raster");
    }

    private static void assertColorAbsent(BufferedImage image, int rgb, String label) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) == rgb) {
                    throw new AssertionError(label + " unexpectedly found in raster");
                }
            }
        }
    }
}
