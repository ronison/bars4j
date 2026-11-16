package org.bars4j.render.linear;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.bars4j.render.RenderSpec;

/**
 * Shared painters for linear symbols: fill a raster and build the SVG path markup from the
 * rectangle run produced by a painting strategy.
 *
 * <p>Every strategy resolves its symbol to {@link BarRect}s; these helpers own the per-format
 * drawing so the strategies do not repeat it.
 */
final class LinearPainters {

    /**
     * Vertical offset of the ink rectangles in the SVG markup relative to the raster: the
     * historical SVG layout draws the bars 2px from the top of a {@code barHeight + 4} px canvas,
     * centred, while the raster draws them from y=0.
     */
    static final int SVG_Y_OFFSET = 2;

    private LinearPainters() {
    }

    /**
     * Creates a canvas of {@code width} x {@code height} filled with the background colour and
     * paints the rectangles of {@code rects} on top.
     */
    static BufferedImage toImage(List<BarRect> rects, int width, int height, RenderSpec spec) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        paint(g2d, 0, 0, width, height, rects, spec);
        g2d.dispose();
        return result;
    }

    /**
     * Paints the background span and the rectangles into an existing raster at the given offset.
     */
    static void renderInto(BufferedImage target, int offsetX, int offsetY, int backgroundWidth,
                           int height, List<BarRect> rects, RenderSpec spec) {
        Graphics2D g2d = target.createGraphics();
        paint(g2d, offsetX, offsetY, backgroundWidth, height, rects, spec);
        g2d.dispose();
    }

    private static void paint(Graphics2D g2d, int offsetX, int offsetY, int backgroundWidth,
                              int height, List<BarRect> rects, RenderSpec spec) {
        if (backgroundWidth > 0) {
            g2d.setBackground(spec.background());
            g2d.setColor(spec.background());
            g2d.fillRect(offsetX, offsetY, backgroundWidth, height);
        }
        for (BarRect rect : rects) {
            g2d.setColor(rect.dark() ? spec.foreground() : spec.background());
            g2d.fillRect(offsetX + rect.x(), offsetY + rect.y(), rect.width(), rect.height());
        }
    }

    /**
     * Builds the SVG markup for the rectangle run: a background path covering the whole canvas
     * (extended {@code backgroundOverhang} px beyond it, the historical 4px text gap) followed by
     * one path per ink colour, with the bars emitted as absolute subpaths.
     *
     * <p>The ink rectangles are shifted {@link #SVG_Y_OFFSET} px down relative to their raster
     * coordinates, reproducing the historical layout.
     *
     * @param canvasWidth  total canvas width in pixels
     * @param canvasHeight total canvas height in pixels
     */
    static String toSvg(List<BarRect> rects, int canvasWidth, int canvasHeight,
                        int backgroundOverhang, RenderSpec spec) {
        StringBuilder svg = new StringBuilder();
        svg.append("<path fill=\"").append(ColorUtil.hex(spec.background()))
                .append("\" d=\"M0 0h").append(canvasWidth)
                .append("v").append(canvasHeight + backgroundOverhang)
                .append("H0z\"/>");
        appendInk(svg, rects, spec, true);
        appendInk(svg, rects, spec, false);
        return svg.toString();
    }

    /**
     * Emits the dark (or light) rectangles as one SVG path. Every rectangle becomes an absolute
     * subpath of the same path element, which keeps the markup compact without relying on the
     * historical run-length structure.
     */
    private static void appendInk(StringBuilder svg, List<BarRect> rects, RenderSpec spec,
                                  boolean dark) {
        boolean any = false;
        for (BarRect rect : rects) {
            if (rect.dark() == dark) {
                any = true;
                break;
            }
        }
        if (!any) {
            return;
        }
        Color ink = dark ? spec.foreground() : spec.background();
        svg.append("<path fill=\"").append(ColorUtil.hex(ink)).append("\" d=\"");
        for (BarRect rect : rects) {
            if (rect.dark() != dark) {
                continue;
            }
            svg.append("M").append(rect.x()).append(" ").append(rect.y() + SVG_Y_OFFSET)
                    .append("h").append(rect.width()).append("v").append(rect.height())
                    .append("h-").append(rect.width()).append("z");
        }
        svg.append("\"/>");
    }
}
