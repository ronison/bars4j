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

    /** Upper bound for the module/bar dimensions, so an untrusted request cannot request a
     * bitmap large enough to exhaust the heap (resource-exhaustion DoS). */
    public static final int MAX_BAR_WIDTH = 100;
    public static final int MAX_BAR_HEIGHT = 10_000;
    public static final int MAX_MODULE_SIZE = 100;

    public RenderSpec {
        if (barWidth <= 0 || barWidth > MAX_BAR_WIDTH) {
            throw new IllegalArgumentException("barWidth must be between 1 and " + MAX_BAR_WIDTH);
        }
        if (barHeight <= 0 || barHeight > MAX_BAR_HEIGHT) {
            throw new IllegalArgumentException("barHeight must be between 1 and " + MAX_BAR_HEIGHT);
        }
        if (moduleSize <= 0 || moduleSize > MAX_MODULE_SIZE) {
            throw new IllegalArgumentException("moduleSize must be between 1 and " + MAX_MODULE_SIZE);
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
