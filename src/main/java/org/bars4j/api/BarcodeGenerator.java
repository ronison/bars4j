package org.bars4j.api;

import java.awt.image.BufferedImage;

import org.bars4j.encode.api.BarcodeException;
import org.bars4j.format.BarcodeFormat;
import org.bars4j.format.BarcodeRegistry;
import org.bars4j.format.FormatSpec;
import org.bars4j.format.LinearBarcodeFormat;
import org.bars4j.format.MatrixBarcodeFormat;
import org.bars4j.render.RenderSpec;
import org.bars4j.symbol.SymbolKind;

/**
 * Unified entry point to generate any registered barcode format (1D or 2D).
 *
 * <pre>{@code
 * BufferedImage img = BarcodeGenerator.of("QRCODE").toImage("Bars4J");
 * String svg = BarcodeGenerator.of("EAN13").toSvg("789100031550");
 * }</pre>
 */
public final class BarcodeGenerator {

    private final BarcodeFormat format;
    private final RenderSpec renderSpec;

    private BarcodeGenerator(BarcodeFormat format, RenderSpec renderSpec) {
        this.format = format;
        this.renderSpec = renderSpec;
    }

    public static BarcodeGenerator of(String formatId) {
        BarcodeFormat format = BarcodeRegistry.getInstance().get(formatId);
        RenderSpec spec = format.spec().kind() == SymbolKind.MATRIX
                ? RenderSpec.matrixDefaults() : RenderSpec.linearDefaults();
        return new BarcodeGenerator(format, spec);
    }

    public BarcodeGenerator with(RenderSpec renderSpec) {
        return new BarcodeGenerator(format, renderSpec);
    }

    public FormatSpec format() {
        return format.spec();
    }

    public BufferedImage toImage(String text) throws BarcodeException {
        return format.toImage(text, renderSpec);
    }

    public String toSvg(String text) throws BarcodeException {
        return format.toSvg(text, renderSpec);
    }

    /**
     * Returns an SVG snippet ready to be embedded inline in an HTML page, at its natural size.
     *
     * <pre>{@code
     * String html = BarcodeGenerator.of("EAN13").toHtml("789100031550");
     * // paste directly inside <body>...</body>
     * }</pre>
     */
    public String toHtml(String text) throws BarcodeException {
        return format.toHtml(text, renderSpec);
    }

    /**
     * Returns an SVG snippet ready to be embedded inline in an HTML page, sized to the given
     * display dimensions in pixels. The {@code viewBox} preserves the symbol geometry, so the
     * barcode scales proportionally.
     */
    public String toHtml(String text, int displayWidth, int displayHeight) throws BarcodeException {
        if (displayWidth <= 0 || displayHeight <= 0) {
            throw new IllegalArgumentException("displayWidth/displayHeight must be positive");
        }
        if (format instanceof LinearBarcodeFormat linear) {
            return linear.toHtml(text, renderSpec, displayWidth, displayHeight);
        }
        if (format instanceof MatrixBarcodeFormat matrix) {
            return matrix.toHtml(text, renderSpec, displayWidth, displayHeight);
        }
        return format.toHtml(text, renderSpec);
    }

    public String checkSum(String text) throws BarcodeException {
        return format.checkSum(text);
    }
}
