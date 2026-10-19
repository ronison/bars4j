package org.bars4j.render;

import java.awt.image.BufferedImage;

import org.bars4j.symbol.BarcodeSymbol;

/**
 * Renders a {@link BarcodeSymbol} to an image or an SVG fragment.
 *
 * <p>{@link #toSvg} returns only the drawing markup (paths/rects); assembling the surrounding
 * SVG document and drawing the human-readable text is the responsibility of the caller/facade.
 */
public interface BarcodeRenderer {

    BufferedImage toImage(BarcodeSymbol symbol, RenderSpec spec);

    String toSvg(BarcodeSymbol symbol, RenderSpec spec);
}
