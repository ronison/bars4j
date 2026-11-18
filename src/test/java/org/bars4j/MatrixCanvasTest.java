package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bars4j.api.BarcodeGenerator;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the SVG/HTML canvas of the matrix (2D) formats: the wrapper used to size
 * both dimensions from {@code widthInModules}, which letterboxed rectangular symbols (PDF417)
 * with a square canvas and dead space below the symbol. The canvas must match the raster's
 * geometry for every matrix format, square or not.
 */
class MatrixCanvasTest {

    private static final Pattern DIMENSIONS = Pattern.compile("width=\"(\\d+)\" height=\"(\\d+)\"");

    @Test
    void pdf417SvgCanvasMatchesRasterGeometry() throws Exception {
        BarcodeGenerator generator = BarcodeGenerator.of("PDF417");
        BufferedImage raster = generator.toImage("https://github.com/ronison/bars4j");
        String svg = generator.toSvg("https://github.com/ronison/bars4j");

        int[] canvas = dimensions(svg);
        assertEquals(raster.getWidth(), canvas[0], "SVG canvas width must match the raster");
        assertEquals(raster.getHeight(), canvas[1], "SVG canvas height must match the raster");
        // The PDF417 symbol is wider than tall: a square canvas would letterbox it.
        assertTrue(canvas[0] != canvas[1], "PDF417 canvas must not be square: " + canvas[0] + "x" + canvas[1]);
    }

    @Test
    void squareMatrixFormatsKeepSquareCanvas() throws Exception {
        for (String id : new String[]{"QRCODE", "DATAMATRIX", "AZTEC"}) {
            BarcodeGenerator generator = BarcodeGenerator.of(id);
            BufferedImage raster = generator.toImage("Bars4J");
            String svg = generator.toSvg("Bars4J");
            int[] canvas = dimensions(svg);
            assertEquals(raster.getWidth(), canvas[0], id + " canvas width must match the raster");
            assertEquals(raster.getHeight(), canvas[1], id + " canvas height must match the raster");
            assertEquals(canvas[0], canvas[1], id + " canvas must stay square");
        }
    }

    @Test
    void pdf417HtmlSnippetMatchesRasterGeometry() throws Exception {
        BarcodeGenerator generator = BarcodeGenerator.of("PDF417");
        BufferedImage raster = generator.toImage("https://github.com/ronison/bars4j");
        String html = generator.toHtml("https://github.com/ronison/bars4j");

        int[] canvas = dimensions(html);
        // The snippet's viewBox (first dimensions occurrence) carries the natural geometry.
        assertEquals(raster.getWidth(), canvas[0], "HTML viewBox width must match the raster");
        assertEquals(raster.getHeight(), canvas[1], "HTML viewBox height must match the raster");
    }

    private static int[] dimensions(String document) {
        Matcher matcher = DIMENSIONS.matcher(document);
        assertTrue(matcher.find(), "no width/height attributes found");
        return new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))};
    }
}
