package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;

import org.bars4j.api.BarcodeGenerator;
import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.linear.Interleaved2of5Encoder;
import org.bars4j.encode.linear.Itf14Encoder;
import org.bars4j.format.BarcodeRegistry;
import org.bars4j.render.RenderSpec;
import org.bars4j.render.linear.Itf14TextRenderer;
import org.bars4j.symbol.BarElement;
import org.bars4j.symbol.LinearSymbol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ITF-14 (GS1 GTIN-14) format: fixed 14-digit length, GS1 mod-10 check digit and the bearer-bar
 * frame in the rendered output.
 */
class Itf14Test {

    private static String bits(LinearSymbol symbol) {
        StringBuilder sb = new StringBuilder();
        for (BarElement e : symbol.elements()) {
            sb.append(e.wide() ? '1' : '0');
        }
        return sb.toString();
    }

    @Test
    @DisplayName("completeLength is 14")
    void completeLength() {
        assertEquals(14, new Itf14Encoder().completeLength());
    }

    @Test
    @DisplayName("GS1 mod-10 check digit vectors")
    void checkSumVectors() throws Exception {
        Itf14Encoder encoder = new Itf14Encoder();
        // Known GS1 GTIN-14 vectors (13 data digits -> check digit).
        assertEquals("3", encoder.computeCheckSum("1540014128876"));
        assertEquals("5", encoder.computeCheckSum("0001234567890"));
        assertEquals("5", encoder.computeCheckSum("1061414100041"));
        // A complete 14-digit code returns the check digit of its 13-digit payload.
        assertEquals("3", encoder.computeCheckSum("15400141288763"));
        // The check digit is only defined over a 13-digit payload.
        assertThrows(InvalidSymbolException.class, () -> encoder.computeCheckSum("987654321092"));
    }

    @Test
    @DisplayName("encoder accepts the 13-digit payload or the complete 14-digit code")
    void acceptsPayloadAndCompleteCode() throws Exception {
        Itf14Encoder encoder = new Itf14Encoder();
        assertNotNull(encoder.encode("1540014128876"));
        assertNotNull(encoder.encode("15400141288763"));
    }

    @Test
    @DisplayName("encoder rejects a wrong check digit, wrong length and non-digits")
    void rejectsInvalidInput() {
        Itf14Encoder encoder = new Itf14Encoder();
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("15400141288764")); // wrong check
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("123456789012"));   // 12 digits
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("123456789012345"));// 15 digits
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("154001412887A"));  // non-digit
    }

    @Test
    @DisplayName("ITF-14 bars are the Interleaved 2 of 5 bars for the same 14-digit code")
    void barsMatchInterleaved2of5() throws Exception {
        String complete = "15400141288763";
        assertEquals(bits(new Interleaved2of5Encoder().encode(complete)),
                bits(new Itf14Encoder().encode(complete)));
    }

    @Test
    @DisplayName("facade renders image and SVG with the bearer-bar frame")
    void facadeRenders() throws Exception {
        BufferedImage image = BarcodeGenerator.of("ITF14").toImage("1540014128876");
        assertNotNull(image);
        assertTrue(image.getWidth() > 0 && image.getHeight() > 0);

        String svg = BarcodeGenerator.of("ITF14").toSvg("1540014128876");
        String interleaved = BarcodeGenerator.of("INTERLEAVED2OF5").toSvg("9876543210921");

        assertNotNull(svg);
        // The Itf14Renderer adds the extra bearer-bar frame path.
        assertEquals(count(svg, "<path"), count(interleaved, "<path") + 1);
    }

    @Test
    @DisplayName("the label uses the ITF-14 HRI mask \"0 00 00000 00000 0\"")
    void labelMask() throws Exception {
        assertEquals("1 54 00141 28876 3", Itf14TextRenderer.format("15400141288763"));
        // the mask is applied in the rendered SVG label as well
        String svg = BarcodeGenerator.of("ITF14").toSvg("1540014128876");
        assertTrue(svg.contains("1 54 00141 28876 3"));
    }

    @Test
    @DisplayName("registry and options expose the ITF-14 format")
    void registryEntry() {
        assertTrue(BarcodeRegistry.getInstance().contains("ITF14"));
        assertEquals("ITF-14", BarcodeRegistry.getInstance().get("ITF14").spec().displayName());
    }

    @Test
    @DisplayName("facade rejects errors like the raw encoder")
    void facadeValidates() {
        assertThrows(InvalidSymbolException.class, () -> BarcodeGenerator.of("ITF14").toImage("15400141288764"));
        assertThrows(InvalidSymbolException.class, () -> BarcodeGenerator.of("ITF14").toImage("123456789012"));
    }

    @Test
    @DisplayName("SVG output is non-empty with default options")
    void svgWithDefaults() throws Exception {
        org.bars4j.format.LinearBarcodeFormat format =
                (org.bars4j.format.LinearBarcodeFormat) BarcodeRegistry.getInstance().get("ITF14");
        String svg = format.toSvg("1540014128876", RenderSpec.linearDefaults());
        assertNotNull(svg);
        assertTrue(svg.contains("<path"));
    }

    @Test
    @DisplayName("bearer bars span the full symbol width, including the quiet zones")
    void bearerBarsSpanFullWidth() throws Exception {
        BufferedImage image = BarcodeGenerator.of("ITF14").toImage("1540014128876");
        int w = image.getWidth();
        int h = image.getHeight();
        boolean top = false;
        boolean bottom = false;
        for (int y = 0; y < h / 2; y++) {
            if (isDark(image, 0, y) && isDark(image, w - 1, y)) {
                top = true;
            }
        }
        for (int y = h / 2; y < h; y++) {
            if (isDark(image, 0, y) && isDark(image, w - 1, y)) {
                bottom = true;
            }
        }
        assertTrue(top, "top bearer bar must reach both edges");
        assertTrue(bottom, "bottom bearer bar must reach both edges");
    }

    private static boolean isDark(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) & 0xFFFFFF) == 0;
    }

    private static int count(String haystack, String needle) {
        int occurrences = 0;
        int index = haystack.indexOf(needle);
        while (index >= 0) {
            occurrences++;
            index = haystack.indexOf(needle, index + needle.length());
        }
        return occurrences;
    }
}
