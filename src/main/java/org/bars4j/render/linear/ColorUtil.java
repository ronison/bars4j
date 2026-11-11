package org.bars4j.render.linear;

import java.awt.Color;

/**
 * SVG support for {@link RenderSpec} colours.
 */
final class ColorUtil {

    private ColorUtil() {
    }

    /** Returns the colour as a {@code #rrggbb} lowercase hexadecimal string. */
    static String hex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
