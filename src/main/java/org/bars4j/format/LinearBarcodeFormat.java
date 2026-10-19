package org.bars4j.format;

import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import org.bars4j.Barcode;
import org.bars4j.encode.InvalidAtributeException;
import org.bars4j.encode.api.BarcodeException;
import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.render.RenderSpec;

/**
 * A linear (1D) format backed by the legacy {@link Barcode} pipeline.
 *
 * <p>This keeps the existing 1D output byte-identical while exposing it through the unified
 * {@link BarcodeFormat} / registry API. The provided {@link RenderSpec} is currently only used by
 * matrix formats; linear formats keep their per-format defaults.
 */
public final class LinearBarcodeFormat implements BarcodeFormat {

    private final Supplier<Barcode> factory;
    private final FormatSpec spec;

    public LinearBarcodeFormat(Supplier<Barcode> factory, FormatSpec spec) {
        this.factory = factory;
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
        try {
            return factory.get().createBarcode(text);
        } catch (InvalidAtributeException e) {
            throw new InvalidSymbolException(e.getMessage(), e);
        }
    }

    @Override
    public String toSvg(String text, RenderSpec renderSpec) throws BarcodeException {
        try {
            return factory.get().createBarcodeSVG(text).toString();
        } catch (InvalidAtributeException e) {
            throw new InvalidSymbolException(e.getMessage(), e);
        }
    }

    @Override
    public String checkSum(String text) throws BarcodeException {
        try {
            return factory.get().calcCheckSum(text);
        } catch (InvalidAtributeException e) {
            throw new InvalidSymbolException(e.getMessage(), e);
        }
    }
}
