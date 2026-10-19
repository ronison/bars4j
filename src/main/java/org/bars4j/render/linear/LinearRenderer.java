package org.bars4j.render.linear;

import org.bars4j.render.BarcodeRenderer;
import org.bars4j.symbol.LinearSymbol;

/**
 * Renderer of {@link LinearSymbol}s.
 *
 * <p>Extends {@link BarcodeRenderer} with the total-width calculation that the SVG assembly needs
 * (the legacy painters exposed the same method as {@code calcTotalWidth}).
 */
public interface LinearRenderer extends BarcodeRenderer {

    int calcTotalWidth(LinearSymbol symbol, int barWidth, int barHeight, double wideRatio);
}
