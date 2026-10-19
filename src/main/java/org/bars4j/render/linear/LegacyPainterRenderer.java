package org.bars4j.render.linear;

import java.awt.image.BufferedImage;

import org.bars4j.encode.linear.BarSetBridge;
import org.bars4j.format.BitSemantics;
import org.bars4j.paint.BarcodePainter;
import org.bars4j.render.BarcodeRenderer;
import org.bars4j.render.RenderSpec;
import org.bars4j.symbol.BarcodeSymbol;
import org.bars4j.symbol.LinearSymbol;

/**
 * Adapts a legacy {@link BarcodePainter} to the new {@link BarcodeRenderer} API.
 *
 * <p>The symbol is converted back to the exact legacy {@code BarSet[]} before delegating, so the
 * rendered output is byte-identical to the pre-refactoring output.
 */
public final class LegacyPainterRenderer implements BarcodeRenderer {

    private final BarcodePainter painter;
    private final BitSemantics semantics;

    public LegacyPainterRenderer(BarcodePainter painter, BitSemantics semantics) {
        this.painter = painter;
        this.semantics = semantics;
    }

    @Override
    public BufferedImage toImage(BarcodeSymbol symbol, RenderSpec spec) {
        return painter.paintToImg(barsOf(symbol), spec.barWidth(), spec.barHeight(), spec.wideRatio());
    }

    @Override
    public String toSvg(BarcodeSymbol symbol, RenderSpec spec) {
        return painter.paintToSVG(barsOf(symbol), spec.barWidth(), spec.barHeight(), spec.wideRatio()).toString();
    }

    private org.bars4j.encode.BarSet[] barsOf(BarcodeSymbol symbol) {
        return BarSetBridge.toBarSets((LinearSymbol) symbol, semantics);
    }
}
