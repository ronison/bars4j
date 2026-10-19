package org.bars4j.api;

import java.awt.image.BufferedImage;

import org.bars4j.encode.api.BarcodeException;
import org.bars4j.format.BarcodeFormat;
import org.bars4j.format.BarcodeRegistry;
import org.bars4j.format.FormatSpec;
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

    public String checkSum(String text) throws BarcodeException {
        return format.checkSum(text);
    }
}
