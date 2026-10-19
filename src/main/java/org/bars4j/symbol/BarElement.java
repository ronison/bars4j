package org.bars4j.symbol;

/**
 * A single element of a linear symbol.
 *
 * @param dark          whether the element is a bar (dark) or a space (light)
 * @param wide          whether the element uses the wide width (wide-to-narrow codes)
 * @param heightPercent element height as a percentage of the full bar height
 *                      (100 for normal codes, 50 for the short bars of PostNet)
 */
public record BarElement(boolean dark, boolean wide, int heightPercent) {

    public BarElement {
        if (heightPercent <= 0) {
            throw new IllegalArgumentException("heightPercent must be positive: " + heightPercent);
        }
    }

    public static BarElement module(boolean dark) {
        return new BarElement(dark, false, 100);
    }
}
