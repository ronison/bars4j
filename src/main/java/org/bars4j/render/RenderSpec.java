package org.bars4j.render;

import java.awt.Color;

/**
 * Rendering options.
 *
 * @param barWidth     width of one narrow module for linear symbols, in pixels
 * @param barHeight    bar height for linear symbols, in pixels
 * @param wideRatio    wide-to-narrow ratio for wide-ratio coded linear symbols
 * @param moduleSize   size of one module for matrix symbols, in pixels
 * @param foreground   bar/module colour
 * @param background   background colour
 */
public record RenderSpec(int barWidth, int barHeight, double wideRatio, int moduleSize,
                         Color foreground, Color background) {

    public RenderSpec {
        if (barWidth <= 0 || barHeight <= 0 || moduleSize <= 0) {
            throw new IllegalArgumentException("barWidth/barHeight/moduleSize must be positive");
        }
        if (wideRatio < 1.0) {
            throw new IllegalArgumentException("wideRatio must be >= 1.0");
        }
    }

    public static RenderSpec linearDefaults() {
        return new RenderSpec(1, 70, 2.0, 4, Color.BLACK, Color.WHITE);
    }

    public static RenderSpec matrixDefaults() {
        return new RenderSpec(1, 70, 2.0, 4, Color.BLACK, Color.WHITE);
    }
}
