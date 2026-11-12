package org.bars4j.render.linear;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

/**
 * Draws the EAN/UPC human-readable digits in fixed-width slots.
 *
 * <p>Each digit is centred inside its own slot (slot width = group width / digit count), so the
 * label geometry matches the SVG output regardless of the font metrics of the runtime platform.
 * Drawing the group as a single string (the previous behaviour) let the last digit drift into the
 * right-hand guard bars whenever the actual font advance exceeded the assumed one.
 *
 * <p>The module width {@code nWidth} may be fractional (the horizontal scale factor) when the
 * destination canvas was scaled; positions are then computed in the same units as the scaled
 * bars so the digits keep lining up with the guard bars.
 */
final class SlotText {

    private SlotText() {
    }

    /** Monospaced font used for the human-readable digits. */
    static Font font(double nWidth, int size) {
        return new Font("monospace", Font.PLAIN, Math.max(1, (int) Math.round(size * nWidth)));
    }

    /**
     * Draws {@code digits} with each glyph centred in its own slot, starting at {@code slotX} and
     * spanning {@code groupWidth} pixels. The baseline is taken from the current font of
     * {@code g}.
     *
     * @param g          graphics with the font already set
     * @param digits     the digits to draw (one glyph per slot)
     * @param slotX      left edge of the first slot
     * @param groupWidth total width shared by the slots
     * @param baseline   text baseline y
     * @param foreground glyph colour
     */
    static void draw(Graphics g, String digits, double slotX, double groupWidth, int baseline, Color foreground) {
        int count = digits.length();
        if (count == 0) {
            return;
        }
        FontMetrics fm = g.getFontMetrics();
        int advance = fm.stringWidth("0");
        double slot = groupWidth / count;
        g.setColor(foreground);
        for (int i = 0; i < count; i++) {
            int x = (int) Math.round(slotX + slot * i + (slot - advance) / 2.0);
            g.drawString(digits.substring(i, i + 1), x, baseline);
        }
    }
}
