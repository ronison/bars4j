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
 * @param quietZone    quiet zone width around the symbol: pixels for linear symbols, modules for
 *                     matrix symbols (0 draws no quiet zone)
 * @param fontSize     font size of the human-readable label for linear symbols, in pixels
 */
public record RenderSpec(int barWidth, int barHeight, double wideRatio, int moduleSize,
                         Color foreground, Color background, int quietZone, int fontSize) {

    /** Upper bound for the module/bar dimensions, so an untrusted request cannot request a
     * bitmap large enough to exhaust the heap (resource-exhaustion DoS). */
    public static final int MAX_BAR_WIDTH = 100;
    public static final int MAX_BAR_HEIGHT = 10_000;
    public static final int MAX_MODULE_SIZE = 100;
    /** Upper bound for the quiet zone (in modules for matrix symbols). */
    public static final int MAX_QUIET_ZONE = 100;
    /** Upper bound for the human-readable label font size. */
    public static final int MAX_FONT_SIZE = 200;

    /** Default quiet zone width (in modules for matrix symbols). */
    public static final int DEFAULT_QUIET_ZONE = 10;
    /** Default font size of the human-readable label. */
    public static final int DEFAULT_FONT_SIZE = 14;

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
        if (quietZone < 0 || quietZone > MAX_QUIET_ZONE) {
            throw new IllegalArgumentException("quietZone must be between 0 and " + MAX_QUIET_ZONE);
        }
        if (fontSize <= 0 || fontSize > MAX_FONT_SIZE) {
            throw new IllegalArgumentException("fontSize must be between 1 and " + MAX_FONT_SIZE);
        }
    }

    /** Four-argument constructor keeping the historical defaults (quiet zone 10, font size 14). */
    public RenderSpec(int barWidth, int barHeight, double wideRatio, int moduleSize,
                      Color foreground, Color background) {
        this(barWidth, barHeight, wideRatio, moduleSize, foreground, background,
                DEFAULT_QUIET_ZONE, DEFAULT_FONT_SIZE);
    }

    public static RenderSpec linearDefaults() {
        return new RenderSpec(1, 70, 2.0, 4, Color.BLACK, Color.WHITE,
                DEFAULT_QUIET_ZONE, DEFAULT_FONT_SIZE);
    }

    public static RenderSpec matrixDefaults() {
        return new RenderSpec(1, 70, 2.0, 4, Color.BLACK, Color.WHITE,
                DEFAULT_QUIET_ZONE, DEFAULT_FONT_SIZE);
    }
}
