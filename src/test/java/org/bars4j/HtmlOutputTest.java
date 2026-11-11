package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.bars4j.api.BarcodeGenerator;
import org.bars4j.render.RenderSpec;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * {@code toHtml} returns an SVG snippet ready to be embedded inline in an HTML page: no XML
 * prologue, no generator comment, no fallback text, with a {@code viewBox}, {@code role="img"}
 * and an {@code aria-label}. The drawing markup inside the snippet is identical to the validated
 * {@code toSvg} output.
 */
class HtmlOutputTest {

    private record Case(String id, String code) {
    }

    private static List<Case> cases() {
        List<Case> cases = new ArrayList<>();
        cases.add(new Case("EAN13", "789100031550"));
        cases.add(new Case("EAN8", "9788515"));
        cases.add(new Case("EAN5", "54495"));
        cases.add(new Case("UPCA", "07567816415"));
        cases.add(new Case("UPCE", "0756785"));
        cases.add(new Case("CODABAR", "97885150040-85"));
        cases.add(new Case("CODE39", "BARS4J-39"));
        cases.add(new Case("CODE39EXT", "Bars4J-39Ext"));
        cases.add(new Case("CODE93", "BARS4J-93"));
        cases.add(new Case("CODE93EXT", "Bars4J-93Ext"));
        cases.add(new Case("CODE128", "Bars4J-128"));
        cases.add(new Case("CODE11", "9788515004041"));
        cases.add(new Case("MSIPLESSEY", "9788515004041"));
        cases.add(new Case("STANDARD2OF5", "978851500404"));
        cases.add(new Case("IATA2OF5", "978851500404"));
        cases.add(new Case("MATRIX2OF5", "123456"));
        cases.add(new Case("DATALOGIC2OF5", "123456"));
        cases.add(new Case("INTERLEAVED2OF5", "9876543210921"));
        cases.add(new Case("ITF14", "1540014128876"));
        cases.add(new Case("POSTNET", "805365961"));
        cases.add(new Case("QRCODE", "Bars4J"));
        cases.add(new Case("DATAMATRIX", "Bars4J"));
        cases.add(new Case("PDF417", "Bars4J"));
        cases.add(new Case("AZTEC", "Bars4J"));
        return cases;
    }

    @TestFactory
    Stream<DynamicTest> htmlSnippetIsEmbeddable() {
        return cases().stream().map(c -> DynamicTest.dynamicTest(c.id(), () -> {
            String html = BarcodeGenerator.of(c.id()).toHtml(c.code());
            assertTrue(html.startsWith("<svg xmlns=\"http://www.w3.org/2000/svg\""),
                    "starts with the svg element: " + html);
            assertFalse(html.contains("<?xml"), "no XML prologue");
            assertFalse(html.contains("Created with Bars4J"), "no generator comment");
            assertFalse(html.contains("Sorry, your browser"), "no fallback text");
            assertTrue(html.contains("role=\"img\""), "img role");
            assertTrue(html.contains("aria-label=\"" + displayName(c.id())), "aria label");
            assertTrue(html.endsWith("</svg>"), "closed svg element");
        }));
    }

    @TestFactory
    Stream<DynamicTest> htmlCarriesViewBox() {
        return cases().stream().map(c -> DynamicTest.dynamicTest(c.id(), () -> {
            String html = BarcodeGenerator.of(c.id()).toHtml(c.code());
            assertTrue(html.contains("viewBox=\"0 0 "), "viewBox present for " + c.id());
            assertTrue(html.contains("preserveAspectRatio=\"xMidYMid meet\""),
                    "standard preserveAspectRatio for " + c.id());
        }));
    }

    /**
     * The key parity guarantee: the drawing markup inside the HTML snippet is identical to the
     * one inside the validated standalone SVG document, so {@code toHtml} never deviates a pixel
     * from the golden-validated output.
     */
    @TestFactory
    Stream<DynamicTest> htmlDrawingMarkupMatchesSvgDocument() {
        return cases().stream().map(c -> DynamicTest.dynamicTest(c.id(), () -> {
            BarcodeGenerator generator = BarcodeGenerator.of(c.id());
            String svg = generator.toSvg(c.code());
            String html = generator.toHtml(c.code());
            assertEquals(drawMarkup(svg), drawMarkup(html),
                    "drawing markup for " + c.id());
        }));
    }

    /** Extracts everything between the end of the opening {@code <svg ...>} tag and {@code </svg>}. */
    private static String drawMarkup(String document) {
        int svgOpen = document.indexOf("<svg");
        int start = document.indexOf('>', svgOpen);
        int end = document.lastIndexOf("</svg>");
        String markup = document.substring(start + 1, end);
        // The standalone document appends a legacy fallback text before the closing tag and (for
        // the matrix formats) carries no <style> element.
        return markup
                .replace("Sorry, your browser does not support inline SVG.", "")
                .replace("<style>text{font-family: 'Courier New', monospace;font-size: 14px;}</style>", "");
    }

    @Test
    void explicitDisplaySizeOverridesWidthAndHeightOnly() throws Exception {
        BarcodeGenerator generator = BarcodeGenerator.of("CODE128");
        String natural = generator.toHtml("B4J");
        String sized = generator.toHtml("B4J", 200, 60);
        assertTrue(sized.contains("width=\"200\" height=\"60\""), "display size applied");
        assertEquals(extractAttribute(natural, "viewBox"), extractAttribute(sized, "viewBox"),
                "viewBox unchanged");
    }

    @Test
    void rejectsNonPositiveDisplaySize() {
        BarcodeGenerator generator = BarcodeGenerator.of("CODE128");
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> generator.toHtml("B4J", 0, 60));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> generator.toHtml("B4J", 200, -1));
    }

    @Test
    void htmlHonoursSpecColors() throws Exception {
        RenderSpec redBlue = new RenderSpec(1, 70, 2.0, 4,
                new Color(0xE5, 0x39, 0x35), new Color(0x0D, 0x47, 0xA1));
        String html = BarcodeGenerator.of("CODE128").with(redBlue).toHtml("B4J");
        assertTrue(html.contains("fill=\"#e53935\""), "foreground hex");
        assertTrue(html.contains("fill=\"#0d47a1\""), "background hex");
    }

    @Test
    void htmlEscapesMarkupCharacters() throws Exception {
        BarcodeGenerator generator = BarcodeGenerator.of("CODE128");
        String html = generator.toHtml("A<B&C>D");
        assertTrue(html.contains("&lt;"), "escaped <");
        assertTrue(html.contains("&amp;"), "escaped &");
        assertTrue(html.contains("&gt;"), "escaped >");
        // and the standalone document gets the same treatment
        assertTrue(generator.toSvg("A<B&C>D").contains("&amp;"), "toSvg escapes too");
    }

    @Test
    void ean13HtmlHasNoLeadingChevron() throws Exception {
        String html = BarcodeGenerator.of("EAN13").toHtml("789100031550");
        assertTrue(html.contains(">315507</text>"), "check digit in the third text group");
        assertEquals(-1, html.indexOf("textLength=\"8\">>"), "no chevron placeholder");
    }

    private static String displayName(String id) {
        return switch (id) {
            case "EAN13" -> "EAN-13";
            case "EAN8" -> "EAN-8";
            case "EAN5" -> "EAN-5";
            case "UPCA" -> "UPC-A";
            case "UPCE" -> "UPC-E";
            case "CODABAR" -> "Codabar";
            case "CODE39" -> "Code 39";
            case "CODE39EXT" -> "Code 39 Extended";
            case "CODE93" -> "Code 93";
            case "CODE93EXT" -> "Code 93 Extended";
            case "CODE128" -> "Code 128";
            case "CODE11" -> "Code 11";
            case "MSIPLESSEY" -> "MSI Plessey";
            case "STANDARD2OF5" -> "Standard 2 of 5";
            case "IATA2OF5" -> "IATA 2 of 5";
            case "MATRIX2OF5" -> "Matrix 2 of 5";
            case "DATALOGIC2OF5" -> "Datalogic 2 of 5";
            case "INTERLEAVED2OF5" -> "Interleaved 2 of 5";
            case "ITF14" -> "ITF-14";
            case "POSTNET" -> "PostNet";
            case "QRCODE" -> "QRCode";
            case "DATAMATRIX" -> "Data Matrix";
            case "PDF417" -> "PDF417";
            case "AZTEC" -> "Aztec";
            default -> throw new IllegalArgumentException(id);
        };
    }

    private static String extractAttribute(String document, String attribute) {
        int from = document.indexOf(attribute + "=\"") + attribute.length() + 2;
        int to = document.indexOf('"', from);
        return document.substring(from, to);
    }
}
