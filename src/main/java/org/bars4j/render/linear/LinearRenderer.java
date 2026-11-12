package org.bars4j.render.linear;

import org.bars4j.render.BarcodeRenderer;
import org.bars4j.render.RenderSpec;
import org.bars4j.symbol.LinearSymbol;

/**
 * Renderer of {@link LinearSymbol}s.
 *
 * <p>Extends {@link BarcodeRenderer} with the total-width calculation that the SVG assembly needs
 * (the historical painters exposed the same method as {@code calcTotalWidth}).
 */
public interface LinearRenderer extends BarcodeRenderer {

    /** Historical default quiet zone (modules per side) used by the width calculation overload. */
    int QUIET_ZONE_MODULES = 10;

    /**
     * Quiet zone width in modules per side (the historical layout reserved 10 modules per side).
     */
    default int quietModules(RenderSpec spec) {
        return spec.quietZone();
    }

    int calcTotalWidth(LinearSymbol symbol, int barWidth, int barHeight, double wideRatio);

    /**
     * Total canvas width for the given quiet zone (modules per side). The default ignores the
     * quiet zone and applies the historical 10-module layout; implementations may override to
     * honour it.
     */
    default int calcTotalWidth(LinearSymbol symbol, int barWidth, int barHeight, double wideRatio,
                               int quietZoneModules) {
        return calcTotalWidth(symbol, barWidth, barHeight, wideRatio);
    }
}
