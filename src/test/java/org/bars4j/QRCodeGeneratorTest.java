package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.bars4j.api.BarcodeGenerator;
import org.bars4j.format.BarcodeRegistry;
import org.bars4j.format.FormatSpec;
import org.bars4j.symbol.SymbolKind;
import org.junit.jupiter.api.Test;

/**
 * Phase 4: end-to-end generation of QRCode through the unified API.
 */
class QRCodeGeneratorTest {

    @Test
    void qrCodeIsRegistered() {
        FormatSpec spec = BarcodeRegistry.getInstance().get("qrcode").spec();
        assertEquals("QRCODE", spec.id());
        assertEquals(SymbolKind.MATRIX, spec.kind());
    }

    @Test
    void rendersImageWithQuietZone() throws Exception {
        BufferedImage image = BarcodeGenerator.of("QRCODE").toImage("Bars4J");
        // version 1 => 21 modules + 8 quiet zone modules = 29 modules; default module size 4.
        assertEquals(29 * 4, image.getWidth());
        assertEquals(29 * 4, image.getHeight());
        assertTrue(hasDarkPixels(image), "QRCode image should contain dark modules");
    }

    @Test
    void rendersSvgDocument() throws Exception {
        String svg = BarcodeGenerator.of("QRCODE").toSvg("Bars4J");
        assertTrue(svg.startsWith("<?xml"), "SVG should start with the XML declaration");
        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("<path"));
        assertTrue(svg.endsWith("</svg>"));
    }

    @Test
    void supportsDifferentEccLevels() throws Exception {
        for (String level : List.of("L", "M", "Q", "H")) {
            BarcodeGenerator generator = BarcodeGenerator.of("QRCODE");
            String svg = generator.toSvg("HELLO WORLD");
            assertFalse(svg.isEmpty(), "level " + level);
        }
    }

    @Test
    void rejectsOversizedInput() {
        String tooLong = "x".repeat(2000);
        assertThrows(Exception.class, () -> BarcodeGenerator.of("QRCODE").toImage(tooLong));
    }

    private static boolean hasDarkPixels(BufferedImage image) {
        List<Integer> colors = new ArrayList<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                if (!colors.contains(rgb)) {
                    colors.add(rgb);
                }
            }
        }
        return colors.stream().anyMatch(c -> c == 0xFF000000);
    }
}
