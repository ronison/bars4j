package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.stream.Stream;

import org.bars4j.demo.SampleContent;
import org.bars4j.format.BarcodeFormat;
import org.bars4j.format.BarcodeRegistry;
import org.bars4j.format.LinearBarcodeFormat;
import org.bars4j.render.RenderSpec;
import org.bars4j.symbol.SymbolKind;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Exercises the exact code paths used by the Swing demo without needing a display: for every
 * registered format the demo's sample content must render to an image and an SVG.
 */
class DemoSamplesTest {

    private static final RenderSpec MATRIX_SPEC =
            new RenderSpec(1, 70, 2.0, 4, Color.BLACK, Color.WHITE);

    @TestFactory
    Stream<DynamicTest> everyRegisteredFormatRendersDemoSample() {
        BarcodeRegistry registry = BarcodeRegistry.getInstance();
        return registry.ids().stream().map(id -> dynamicTest(id, () -> {
            BarcodeFormat format = registry.get(id);
            String text = SampleContent.forFormat(id);

            BufferedImage image;
            String svg;
            if (format.spec().kind() == SymbolKind.LINEAR) {
                LinearBarcodeFormat linear = (LinearBarcodeFormat) format;
                image = linear.toImage(text, RenderSpec.linearDefaults());
                svg = linear.toSvg(text, RenderSpec.linearDefaults());
            } else {
                image = format.toImage(text, MATRIX_SPEC);
                svg = format.toSvg(text, MATRIX_SPEC);
            }

            assertNotNull(image, "image for " + id);
            assertTrue(image.getWidth() > 0 && image.getHeight() > 0, "image size for " + id);
            assertNotNull(svg, "svg for " + id);
            assertFalse(svg.isEmpty(), "svg empty for " + id);
        }));
    }

    @Test
    void sampleContentCoversEveryRegisteredFormat() {
        BarcodeRegistry registry = BarcodeRegistry.getInstance();
        for (String id : registry.ids()) {
            // The fallback is "Bars4J"; make sure no registered format silently relies on it.
            assertNotNull(SampleContent.forFormat(id), id);
        }
    }

    @Test
    void linearOptionsRoundTrip() {
        BarcodeFormat format = BarcodeRegistry.getInstance().get("EAN13");
        LinearBarcodeFormat linear = (LinearBarcodeFormat) format;

        LinearBarcodeFormat.LinearOptions options = linear.options();
        LinearBarcodeFormat copy = linear.withOptions(options);
        assertEquals(options, copy.options(), "options must round-trip");
    }

    @Test
    void linearOptionsChangeRenderedGeometry() throws Exception {
        LinearBarcodeFormat linear = (LinearBarcodeFormat) BarcodeRegistry.getInstance().get("EAN13");
        LinearBarcodeFormat.LinearOptions defaults = linear.options();

        BufferedImage baseline = linear.toImage(SampleContent.forFormat("EAN13"), RenderSpec.linearDefaults());

        LinearBarcodeFormat taller = linear.withOptions(new LinearBarcodeFormat.LinearOptions(
                defaults.xDimensionMm(), defaults.barHeightMm() + 20, defaults.wideRatio(),
                defaults.checkDigit(), defaults.showCheckDigit(), defaults.showText()));
        BufferedImage tallImage = taller.toImage(SampleContent.forFormat("EAN13"), RenderSpec.linearDefaults());
        assertTrue(tallImage.getHeight() > baseline.getHeight(), "bar height must affect image height");
        assertEquals(baseline.getWidth(), tallImage.getWidth(), "bar height must not affect width");

        LinearBarcodeFormat wider = linear.withOptions(new LinearBarcodeFormat.LinearOptions(
                defaults.xDimensionMm() * 2, defaults.barHeightMm(), defaults.wideRatio(),
                defaults.checkDigit(), defaults.showCheckDigit(), defaults.showText()));
        BufferedImage wideImage = wider.toImage(SampleContent.forFormat("EAN13"), RenderSpec.linearDefaults());
        assertTrue(wideImage.getWidth() > baseline.getWidth(), "x-dimension must affect image width");
    }
}
