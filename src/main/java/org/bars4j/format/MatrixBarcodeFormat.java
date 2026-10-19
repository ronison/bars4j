package org.bars4j.format;

import java.awt.image.BufferedImage;

import org.bars4j.encode.api.BarcodeException;
import org.bars4j.encode.api.MatrixEncoder;
import org.bars4j.render.BarcodeRenderer;
import org.bars4j.render.RenderSpec;
import org.bars4j.symbol.MatrixSymbol;

/**
 * A matrix (2D) format backed by the new symbol/renderer pipeline (e.g. QRCode).
 */
public final class MatrixBarcodeFormat implements BarcodeFormat {

    private final MatrixEncoder encoder;
    private final BarcodeRenderer renderer;
    private final FormatSpec spec;

    public MatrixBarcodeFormat(MatrixEncoder encoder, BarcodeRenderer renderer, FormatSpec spec) {
        this.encoder = encoder;
        this.renderer = renderer;
        this.spec = spec;
    }

    @Override
    public String id() {
        return spec.id();
    }

    @Override
    public FormatSpec spec() {
        return spec;
    }

    @Override
    public BufferedImage toImage(String text, RenderSpec renderSpec) throws BarcodeException {
        return renderer.toImage(encoder.encode(text), renderSpec);
    }

    @Override
    public String toSvg(String text, RenderSpec renderSpec) throws BarcodeException {
        MatrixSymbol symbol = encoder.encode(text);
        String fragment = renderer.toSvg(symbol, renderSpec);
        int total = (symbol.widthInModules() + 2 * symbol.quietZone()) * renderSpec.moduleSize();
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n"
                + "<!-- Created with Bars4J -->\n"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.2\" width=\"" + total
                + "\" height=\"" + total + "\" viewBox=\"0 0 " + total + " " + total + "\">"
                + fragment
                + "</svg>";
    }

    @Override
    public String checkSum(String text) throws BarcodeException {
        return encoder.computeCheckSum(text);
    }
}
