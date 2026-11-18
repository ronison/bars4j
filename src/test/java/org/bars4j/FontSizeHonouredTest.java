package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import org.bars4j.api.BarcodeGenerator;
import org.bars4j.encode.api.BarcodeException;
import org.bars4j.render.RenderSpec;
import org.junit.jupiter.api.Test;

/**
 * The {@code fontSize} of the {@link RenderSpec} must be honoured by every output path: the SVG
 * document style, the HTML snippet style and the raster label. (Historically only {@code toSvg}
 * applied it; {@code toHtml} hardcoded 14px and the raster label used a fixed size, so the Swing
 * demo's label-font spinner had no visual effect.)
 */
class FontSizeHonouredTest {

    private static final RenderSpec SPEC_10 =
            new RenderSpec(1, 70, 2.0, 4, Color.BLACK, Color.WHITE, 10, 10);
    private static final RenderSpec SPEC_20 =
            new RenderSpec(1, 70, 2.0, 4, Color.BLACK, Color.WHITE, 10, 20);

    @Test
    void svgStyleReflectsFontSize() throws BarcodeException {
        String svg10 = BarcodeGenerator.of("EAN13").with(SPEC_10).toSvg("789100031550");
        String svg20 = BarcodeGenerator.of("EAN13").with(SPEC_20).toSvg("789100031550");
        assertTrue(svg10.contains("font-size: 10px"), "toSvg style must use the spec font size");
        assertTrue(svg20.contains("font-size: 20px"), "toSvg style must use the spec font size");
    }

    @Test
    void svgCanvasHeightGrowsWithFontSize() throws BarcodeException {
        String svg10 = BarcodeGenerator.of("EAN13").with(SPEC_10).toSvg("789100031550");
        String svg20 = BarcodeGenerator.of("EAN13").with(SPEC_20).toSvg("789100031550");
        assertNotEquals(extractHeight(svg10), extractHeight(svg20),
                "toSvg canvas height must grow with the font size");
    }

    @Test
    void htmlStyleReflectsFontSize() throws BarcodeException {
        String html10 = BarcodeGenerator.of("EAN13").with(SPEC_10).toHtml("789100031550");
        String html20 = BarcodeGenerator.of("EAN13").with(SPEC_20).toHtml("789100031550");
        assertTrue(html10.contains("font-size: 10px"), "toHtml style must use the spec font size");
        assertTrue(html20.contains("font-size: 20px"), "toHtml style must use the spec font size");
    }

    @Test
    void rasterLabelIsRenderedWithTheSpecFontSize() throws BarcodeException {
        // A bigger label ink cannot fit into the same pixels: compare the label band of the two
        // images rendered with different font sizes (10 and 30 to make the difference unambiguous).
        RenderSpec spec10 = new RenderSpec(1, 70, 2.0, 4, Color.BLACK, Color.WHITE, 10, 10);
        RenderSpec spec30 = new RenderSpec(1, 70, 2.0, 4, Color.BLACK, Color.WHITE, 10, 30);
        var img10 = BarcodeGenerator.of("EAN13").with(spec10).toImage("789100031550");
        var img30 = BarcodeGenerator.of("EAN13").with(spec30).toImage("789100031550");

        assertEquals(img10.getWidth(), img30.getWidth(), "font size must not change the width");
        assertEquals(img10.getHeight(), img30.getHeight(), "font size must not change the canvas height");
        assertNotEquals(labelBandFingerprint(img10), labelBandFingerprint(img30),
                "the label band pixels must differ when the font size changes");
    }

    @Test
    void defaultFontSizeIsAppliedWhenUnspecified() throws BarcodeException {
        // The 6-arg constructor keeps the historical defaults: the style must carry 14px.
        String svg = BarcodeGenerator.of("EAN13").toSvg("789100031550");
        assertTrue(svg.contains("font-size: 14px"), "unspecified font size must default to 14px");
    }

    @Test
    void rasterResolvesAPhysicalMonoFamily() {
        // The AWT logical "monospace" maps to the synthesised Dialog fallback on some platforms,
        // whose small-size glyphs rasterise jagged even with anti-aliasing. The chain must resolve
        // a physical, well-hinted family when one is installed (falls back to "monospace" otherwise).
        String resolved = org.bars4j.render.linear.SlotTextAccess.resolveFamily();
        assertNotEquals("Dialog", new java.awt.Font(resolved, java.awt.Font.PLAIN, 12).getFamily(),
                "the raster label font must not resolve to the Dialog synthesised fallback");
    }

    /** Simple fingerprint of the label band (the strip below the bars holding the digits). */
    private static long labelBandFingerprint(java.awt.image.BufferedImage image) {
        long hash = 17;
        for (int y = 70; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                hash = hash * 31 + image.getRGB(x, y);
            }
        }
        return hash;
    }

    private static String extractHeight(String svg) {
        int from = svg.indexOf("height=\"") + "height=\"".length();
        return svg.substring(from, svg.indexOf('"', from));
    }
}
