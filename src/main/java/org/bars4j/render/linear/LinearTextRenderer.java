package org.bars4j.render.linear;

import java.awt.image.BufferedImage;

import org.bars4j.render.RenderSpec;

/**
 * Draws the human-readable text of a linear barcode, natively reproducing the legacy
 * {@code org.bars4j.paint.TextPainter} implementations for both raster and SVG output.
 */
public interface LinearTextRenderer {

    /** Draws the text onto an already rendered barcode image. */
    void paintText(BufferedImage image, String text, int nWidth);

    /**
     * Draws the text onto an already rendered barcode image, clearing the text area with the
     * given spec's colours. The default delegates to {@link #paintText(BufferedImage, String, int)}
     * (colour-unaware); implementations may override to honour the spec.
     */
    default void paintText(BufferedImage image, String text, int nWidth, RenderSpec spec) {
        paintText(image, text, nWidth);
    }

    /** Appends the SVG markup for the text area (may assume {@code fontSize} of 14). */
    String paintTextSvg(String text, int barHeight, int totalWidth);

    int calcTotalHeight(int barHeight, int fontSize);
}
