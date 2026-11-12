package org.bars4j.render.linear;

import java.awt.image.BufferedImage;

import org.bars4j.render.RenderSpec;

/**
 * Draws the human-readable text of a linear barcode for both raster and SVG output.
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

    /**
     * Draws the text onto an already rendered barcode image whose bars were drawn with a module
     * width of {@code nWidth} pixels — fractional when the canvas was horizontally scaled (the
     * value is then the scale factor). The default delegates to
     * {@link #paintText(BufferedImage, String, int, RenderSpec)} with the width rounded to the
     * nearest pixel; implementations may override to keep sub-pixel slot positions aligned with
     * the scaled bars.
     */
    default void paintText(BufferedImage image, String text, double nWidth, RenderSpec spec) {
        paintText(image, text, (int) Math.round(nWidth), spec);
    }

    /** Appends the SVG markup for the text area (may assume {@code fontSize} of 14). */
    String paintTextSvg(String text, int barHeight, int totalWidth);

    /**
     * Appends the SVG markup for the text area, clearing the label background with the given
     * spec's colours. The default ignores the spec (hardcoded {@code #fff});
     * implementations may override to honour it.
     */
    default String paintTextSvg(String text, int barHeight, int totalWidth, RenderSpec spec) {
        return paintTextSvg(text, barHeight, totalWidth);
    }

    int calcTotalHeight(int barHeight, int fontSize);
}
