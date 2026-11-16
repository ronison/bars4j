package org.bars4j.render.linear;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.bars4j.render.RenderSpec;
import org.bars4j.symbol.BarcodeSymbol;
import org.bars4j.symbol.LinearSymbol;

/**
 * Height-coded rendering strategy (bit = full-height bar, 0 = short bar), natively reproducing the
 * (e.g. PostNet) for both raster and SVG output.
 *
 * <p>The symbol is resolved to a run of {@link BarRect}s, which the shared {@link LinearPainters}
 * turn into raster or SVG output. Light elements are not painted (the background span shows
 * through), and every element advances by two module widths, the historical PostNet pitch.
 */
public final class HeightCodedRenderer implements LinearRenderer {

    @Override
    public BufferedImage toImage(BarcodeSymbol symbol, RenderSpec spec) {
        LinearSymbol linear = (LinearSymbol) symbol;
        int barWidth = spec.barWidth();
        int barHeight = spec.barHeight();
        int quiet = quietModules(spec);
        List<BarRect> rects = rects(linear, barWidth, barHeight, quiet);
        return LinearPainters.toImage(rects, totalWidth(linear, barWidth, quiet), barHeight, spec);
    }

    @Override
    public void renderInto(BufferedImage target, int offsetX, int offsetY, BarcodeSymbol symbol, RenderSpec spec) {
        LinearSymbol linear = (LinearSymbol) symbol;
        int barWidth = spec.barWidth();
        int barHeight = spec.barHeight();
        int quiet = quietModules(spec);
        List<BarRect> rects = rects(linear, barWidth, barHeight, quiet);
        LinearPainters.renderInto(target, offsetX, offsetY, totalWidth(linear, barWidth, quiet),
                barHeight, rects, spec);
    }

    @Override
    public String toSvg(BarcodeSymbol symbol, RenderSpec spec) {
        LinearSymbol linear = (LinearSymbol) symbol;
        int barWidth = spec.barWidth();
        int barHeight = spec.barHeight();
        int quiet = quietModules(spec);
        List<BarRect> rects = rects(linear, barWidth, barHeight, quiet);
        return LinearPainters.toSvg(rects, totalWidth(linear, barWidth, quiet), barHeight,
                WidthCodedRenderer.BACKGROUND_OVERHANG, spec);
    }

    @Override
    public int calcTotalWidth(LinearSymbol symbol, int barWidth, int barHeight, double wideRatio) {
        return calcTotalWidth(symbol, barWidth, barHeight, wideRatio, QUIET_ZONE_MODULES);
    }

    @Override
    public int calcTotalWidth(LinearSymbol symbol, int barWidth, int barHeight, double wideRatio,
                              int quietZoneModules) {
        return totalWidth(symbol, barWidth, quietZoneModules);
    }

    /**
     * Resolves the symbol to rectangles: dark elements are full-height bars, light elements are
     * half-height bars, both bottom-aligned at the bar height (the historical painter's rule). The
     * SVG painter shifts the run {@link LinearPainters#SVG_Y_OFFSET} px down, which puts the bar
     * baseline 2px above the canvas bottom as before.
     */
    private static List<BarRect> rects(LinearSymbol symbol, int barWidth, int barHeight, int quiet) {
        List<BarRect> rects = new ArrayList<>();
        int x = quiet * barWidth; // leading quiet zone
        for (boolean bit : SymbolBits.dark(symbol)) {
            int unit = bit ? barHeight : barHeight / 2;
            rects.add(new BarRect(x, barHeight - unit, barWidth, unit, true));
            x += 2 * barWidth;
        }
        return rects;
    }

    private static int totalWidth(LinearSymbol symbol, int barWidth, int quiet) {
        // Every element advances by two module widths; the last one does not.
        return (symbol.elements().size() * barWidth * 2) - barWidth + (2 * quiet * barWidth);
    }
}
