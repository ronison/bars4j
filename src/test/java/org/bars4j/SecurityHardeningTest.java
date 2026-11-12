package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.bars4j.api.BarcodeGenerator;
import org.bars4j.encode.api.BarcodeException;
import org.bars4j.format.BarcodeFormat;
import org.bars4j.format.BarcodeRegistry;
import org.bars4j.format.LinearBarcodeFormat;
import org.bars4j.format.MatrixBarcodeFormat;
import org.bars4j.render.RenderSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the security hardening: resource-exhaustion limits (RenderSpec dimension
 * bounds, variable-length text cap), the no-shadowing rule of the registry and the defensive
 * padding of the EAN/UPC human-readable label when the check digit is not shown.
 */
class SecurityHardeningTest {

    @AfterEach
    void restoreLimits() {
        LinearBarcodeFormat.resetMaxTextLength();
    }

    // -------------------------------------------------------------------------------------------
    // RenderSpec dimension bounds (resource-exhaustion guard)
    // -------------------------------------------------------------------------------------------

    @Test
    void renderSpecRejectsOversizedDimensions() {
        assertThrows(IllegalArgumentException.class, () -> new RenderSpec(1, 70, 2.0, 101, Color.BLACK, Color.WHITE));
        assertThrows(IllegalArgumentException.class, () -> new RenderSpec(101, 70, 2.0, 4, Color.BLACK, Color.WHITE));
        assertThrows(IllegalArgumentException.class, () -> new RenderSpec(1, 10_001, 2.0, 4, Color.BLACK, Color.WHITE));
    }

    @Test
    void renderSpecAcceptsBoundaryDimensions() {
        assertNotNull(new RenderSpec(100, 10_000, 2.0, 100, Color.BLACK, Color.WHITE));
    }

    @Test
    void oversizedModuleSizeThroughThePublicApiIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> BarcodeGenerator.of("QRCODE").with(new RenderSpec(1, 70, 2.0, 1_000_000, Color.BLACK, Color.WHITE))
                        .toImage("Bars4J"));
    }

    // -------------------------------------------------------------------------------------------
    // Variable-length text cap
    // -------------------------------------------------------------------------------------------

    @Test
    void variableLengthTextBeyondTheCapIsRejected() {
        assertThrows(BarcodeException.class,
                () -> BarcodeGenerator.of("CODE128").toImage("A".repeat(LinearBarcodeFormat.DEFAULT_MAX_TEXT_LENGTH + 1)));
    }

    @Test
    void variableLengthTextAtTheCapStillWorks() throws Exception {
        assertNotNull(BarcodeGenerator.of("CODE128").toImage("A".repeat(LinearBarcodeFormat.DEFAULT_MAX_TEXT_LENGTH)));
    }

    @Test
    void textCapIsConfigurable() throws Exception {
        LinearBarcodeFormat.setMaxTextLength(10);
        assertThrows(BarcodeException.class, () -> BarcodeGenerator.of("CODE128").toImage("A".repeat(11)));
        assertNotNull(BarcodeGenerator.of("CODE128").toImage("A".repeat(10)));
    }

    @Test
    void invalidTextCapValuesAreIgnored() throws Exception {
        LinearBarcodeFormat.setMaxTextLength(0);
        assertEquals(LinearBarcodeFormat.DEFAULT_MAX_TEXT_LENGTH, LinearBarcodeFormat.maxTextLength());
        LinearBarcodeFormat.setMaxTextLength(-5);
        assertEquals(LinearBarcodeFormat.DEFAULT_MAX_TEXT_LENGTH, LinearBarcodeFormat.maxTextLength());
    }

    // -------------------------------------------------------------------------------------------
    // Registry no-shadowing rule
    // -------------------------------------------------------------------------------------------

    @Test
    void registryRejectsReplacingABuiltInFormat() {
        BarcodeRegistry registry = BarcodeRegistry.getInstance();
        BarcodeFormat existing = registry.get("QRCODE");
        assertThrows(IllegalArgumentException.class, () -> registry.register(existing));
    }

    @Test
    void registryStillAcceptsCustomFormatIds() {
        BarcodeRegistry registry = BarcodeRegistry.getInstance();
        // A fresh custom id is not registered by default; the built-ins are untouched.
        assertTrue(!registry.contains("MYQRCODE"));
        assertTrue(registry.contains("EAN13"));
    }

    // -------------------------------------------------------------------------------------------
    // Defensive padding of the EAN/UPC label without the check digit
    // -------------------------------------------------------------------------------------------

    @Test
    void ean13HtmlWithoutShownCheckDigitDoesNotCrash() throws Exception {
        LinearBarcodeFormat format = (LinearBarcodeFormat) BarcodeRegistry.getInstance().get("EAN13");
        LinearBarcodeFormat.LinearOptions o = format.options();
        LinearBarcodeFormat noShow = format.withOptions(new LinearBarcodeFormat.LinearOptions(
                o.xDimensionMm(), o.barHeightMm(), o.wideRatio(), o.checkDigit(), false, o.showText()));
        // Label is 12 digits (check digit suppressed): the EAN-13 text renderer used to crash with
        // StringIndexOutOfBoundsException on the 13th digit.
        String html = noShow.toHtml("789100031550", org.bars4j.render.RenderSpec.linearDefaults());
        assertTrue(html.startsWith("<svg"), html);
        assertTrue(html.contains("7891000"));
    }

    @Test
    void upcaAndUpceHtmlWithoutShownCheckDigitDoNotCrash() throws Exception {
        for (String id : new String[]{"UPCA", "UPCE"}) {
            LinearBarcodeFormat format = (LinearBarcodeFormat) BarcodeRegistry.getInstance().get(id);
            LinearBarcodeFormat.LinearOptions o = format.options();
            LinearBarcodeFormat noShow = format.withOptions(new LinearBarcodeFormat.LinearOptions(
                    o.xDimensionMm(), o.barHeightMm(), o.wideRatio(), o.checkDigit(), false, o.showText()));
            assertNotNull(noShow.toHtml("07567816415".substring(0, id.equals("UPCA") ? 11 : 7),
                    org.bars4j.render.RenderSpec.linearDefaults()));
        }
    }

    // -------------------------------------------------------------------------------------------
    // Markup characters in the code are escaped in the SVG
    // -------------------------------------------------------------------------------------------

    @Test
    void facadeEscapesMarkupCharacters() throws Exception {
        String svg = BarcodeGenerator.of("CODE128").toSvg("A<B&C>D");
        assertTrue(svg.contains("&lt;"), "escaped <");
        assertTrue(svg.contains("&amp;"), "escaped &");
        assertTrue(svg.contains("&gt;"), "escaped >");
    }
}
