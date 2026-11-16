package org.bars4j.render.linear;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.bars4j.render.RenderSpec;
import org.bars4j.symbol.BarcodeSymbol;
import org.bars4j.symbol.LinearSymbol;

/**
 * Wide-to-narrow ratio rendering strategy (bit = wide element, colour alternates starting dark),
 * reproducing the historical {@code WideRatioCodedPainter} strategy for both raster and SVG output.
 *
 * <p>The symbol is resolved to a run of {@link BarRect}s, which the shared {@link LinearPainters}
 * turn into raster or SVG output. The alternating ink (dark/light, starting dark) reproduces the
 * historical painter exactly: wide-ratio codes draw every other element dark, irrespective of the
 * element's own dark flag.
 */
public class WideRatioCodedRenderer implements LinearRenderer {

    @Override
    public BufferedImage toImage(BarcodeSymbol symbol, RenderSpec spec) {
        LinearSymbol linear = (LinearSymbol) symbol;
        int barWidth = spec.barWidth();
        int barHeight = spec.barHeight();
        double wideRatio = spec.wideRatio();
        List<BarRect> rects = rects(linear, barWidth, barHeight, wideRatio, quietModules(spec));
        return LinearPainters.toImage(rects, totalWidth(linear, barWidth, wideRatio, quietModules(spec)),
                barHeight, spec);
    }

    @Override
    public void renderInto(BufferedImage target, int offsetX, int offsetY, BarcodeSymbol symbol, RenderSpec spec) {
        LinearSymbol linear = (LinearSymbol) symbol;
        int barWidth = spec.barWidth();
        int barHeight = spec.barHeight();
        double wideRatio = spec.wideRatio();
        int quiet = quietModules(spec);
        List<BarRect> rects = rects(linear, barWidth, barHeight, wideRatio, quiet);
        LinearPainters.renderInto(target, offsetX, offsetY,
                totalWidth(linear, barWidth, wideRatio, quiet), barHeight, rects, spec);
    }

    @Override
    public String toSvg(BarcodeSymbol symbol, RenderSpec spec) {
        LinearSymbol linear = (LinearSymbol) symbol;
        int barWidth = spec.barWidth();
        int barHeight = spec.barHeight();
        double wideRatio = spec.wideRatio();
        int quiet = quietModules(spec);
        List<BarRect> rects = rects(linear, barWidth, barHeight, wideRatio, quiet);
        return LinearPainters.toSvg(rects, totalWidth(linear, barWidth, wideRatio, quiet), barHeight,
                WidthCodedRenderer.BACKGROUND_OVERHANG, spec);
    }

    @Override
    public int calcTotalWidth(LinearSymbol symbol, int barWidth, int barHeight, double wideRatio) {
        return calcTotalWidth(symbol, barWidth, barHeight, wideRatio, QUIET_ZONE_MODULES);
    }

    @Override
    public int calcTotalWidth(LinearSymbol symbol, int barWidth, int barHeight, double wideRatio,
                              int quietZoneModules) {
        return totalWidth(symbol, barWidth, wideRatio, quietZoneModules);
    }

    /**
     * Resolves the symbol to rectangles: wide elements use the rounded wide width, and the ink
     * alternates dark/light starting dark (the historical painter's rule).
     */
    private static List<BarRect> rects(LinearSymbol symbol, int barWidth, int barHeight,
                                       double wideRatio, int quiet) {
        List<BarRect> rects = new ArrayList<>();
        int wideWidth = (int) Math.round(barWidth * wideRatio);
        int x = quiet * barWidth; // leading quiet zone
        boolean dark = true;
        for (boolean bit : SymbolBits.wide(symbol)) {
            int width = bit ? wideWidth : barWidth;
            rects.add(new BarRect(x, 0, width, barHeight, dark));
            dark = !dark;
            x += width;
        }
        return rects;
    }

    private static int totalWidth(LinearSymbol symbol, int barWidth, double wideRatio, int quiet) {
        int wideWidth = (int) Math.round(barWidth * wideRatio);
        int width = 2 * quiet * barWidth; // quiet zone on each side
        for (boolean bit : SymbolBits.wide(symbol)) {
            width += bit ? wideWidth : barWidth;
        }
        return width;
    }
}
