package org.bars4j.render.linear;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.bars4j.render.RenderSpec;
import org.bars4j.symbol.BarcodeSymbol;
import org.bars4j.symbol.LinearSymbol;

/**
 * Width-coded rendering strategy (bit = dark module), reproducing the
 * {@code WidthCodedPainter} for both raster and SVG output.
 *
 * <p>Colours come from the {@link RenderSpec} (foreground for dark modules, background for the
 * light modules and the canvas). The symbol is resolved to a run of {@link BarRect}s, which the
 * shared {@link LinearPainters} turn into raster or SVG output.
 */
public final class WidthCodedRenderer implements LinearRenderer {

    /** Extra canvas height below the bars (the historical 4px text gap). */
    static final int BACKGROUND_OVERHANG = 4;

    @Override
    public BufferedImage toImage(BarcodeSymbol symbol, RenderSpec spec) {
        LinearSymbol linear = (LinearSymbol) symbol;
        int barWidth = spec.barWidth();
        int barHeight = spec.barHeight();
        int quiet = quietModules(spec);
        List<BarRect> rects = rects(linear, barWidth, barHeight, quiet);
        return LinearPainters.toImage(rects, totalWidth(linear.elements().size(), barWidth, quiet),
                barHeight, spec);
    }

    @Override
    public void renderInto(BufferedImage target, int offsetX, int offsetY, BarcodeSymbol symbol, RenderSpec spec) {
        LinearSymbol linear = (LinearSymbol) symbol;
        int barWidth = spec.barWidth();
        int barHeight = spec.barHeight();
        int quiet = quietModules(spec);
        List<BarRect> rects = rects(linear, barWidth, barHeight, quiet);
        LinearPainters.renderInto(target, offsetX, offsetY,
                totalWidth(linear.elements().size(), barWidth, quiet), barHeight, rects, spec);
    }

    @Override
    public String toSvg(BarcodeSymbol symbol, RenderSpec spec) {
        LinearSymbol linear = (LinearSymbol) symbol;
        int barWidth = spec.barWidth();
        int barHeight = spec.barHeight();
        int quiet = quietModules(spec);
        List<BarRect> rects = rects(linear, barWidth, barHeight, quiet);
        return LinearPainters.toSvg(rects, totalWidth(linear.elements().size(), barWidth, quiet),
                barHeight, BACKGROUND_OVERHANG, spec);
    }

    @Override
    public int calcTotalWidth(LinearSymbol symbol, int barWidth, int barHeight, double wideRatio) {
        return calcTotalWidth(symbol, barWidth, barHeight, wideRatio, QUIET_ZONE_MODULES);
    }

    @Override
    public int calcTotalWidth(LinearSymbol symbol, int barWidth, int barHeight, double wideRatio,
                              int quietZoneModules) {
        return totalWidth(symbol.elements().size(), barWidth, quietZoneModules);
    }

    /**
     * Resolves the symbol to rectangles: one module per element, dark elements in the foreground
     * colour, light elements painted in the background colour (so they mask the background span).
     */
    private static List<BarRect> rects(LinearSymbol symbol, int barWidth, int barHeight, int quiet) {
        List<BarRect> rects = new ArrayList<>();
        int x = quiet * barWidth; // leading quiet zone
        for (boolean bit : SymbolBits.dark(symbol)) {
            rects.add(new BarRect(x, 0, barWidth, barHeight, bit));
            x += barWidth;
        }
        return rects;
    }

    private static int totalWidth(int elements, int barWidth, int quiet) {
        return elements * barWidth + 2 * quiet * barWidth; // quiet zone on each side
    }
}
