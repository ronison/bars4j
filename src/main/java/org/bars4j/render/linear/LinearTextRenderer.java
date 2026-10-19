package org.bars4j.render.linear;

import java.awt.image.BufferedImage;

/**
 * Draws the human-readable text of a linear barcode, natively reproducing the legacy
 * {@code org.bars4j.paint.TextPainter} implementations for both raster and SVG output.
 */
public interface LinearTextRenderer {

    /** Draws the text onto an already rendered barcode image. */
    void paintText(BufferedImage image, String text, int nWidth);

    /** Appends the SVG markup for the text area (may assume {@code fontSize} of 14). */
    String paintTextSvg(String text, int barHeight, int totalWidth);

    int calcTotalHeight(int barHeight, int fontSize);
}
