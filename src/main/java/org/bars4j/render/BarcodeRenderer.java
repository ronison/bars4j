package org.bars4j.render;

import java.awt.Graphics2D;
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

    /**
     * Draws the symbol into an existing target image at the given offset, without allocating an
     * intermediate {@link BufferedImage}. Callers that already own the destination raster (the
     * linear facade, when no scaling is required) can use this to avoid a full-size copy.
     *
     * <p>The default implementation falls back to {@link #toImage} and blits the result, so
     * implementations that do not care about the extra allocation need not override it.
     */
    default void renderInto(BufferedImage target, int offsetX, int offsetY, BarcodeSymbol symbol, RenderSpec spec) {
        BufferedImage image = toImage(symbol, spec);
        Graphics2D g = target.createGraphics();
        g.drawImage(image, offsetX, offsetY, null);
        g.dispose();
    }
}
