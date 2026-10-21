package org.bars4j.format;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.bars4j.encode.matrix.AztecEncoder;
import org.bars4j.encode.matrix.DataMatrixEncoder;
import org.bars4j.encode.matrix.PDF417Encoder;
import org.bars4j.encode.matrix.QRCodeEncoder;
import org.bars4j.encode.matrix.qr.QrEccLevel;
import org.bars4j.render.matrix.MatrixRenderer;
import org.bars4j.symbol.SymbolKind;

/**
 * Registry of the barcode formats known to the library. Formats can be looked up by id
 * (case-insensitive) and new formats can be registered at runtime.
 */
public final class BarcodeRegistry {

    private static final BarcodeRegistry INSTANCE = new BarcodeRegistry();

    private final Map<String, BarcodeFormat> formats = new LinkedHashMap<>();

    private BarcodeRegistry() {
        BuiltinFormats.register(this);
    }

    public static BarcodeRegistry getInstance() {
        return INSTANCE;
    }

    public void register(BarcodeFormat format) {
        formats.put(normalize(format.id()), format);
    }

    public BarcodeFormat get(String id) {
        BarcodeFormat format = formats.get(normalize(id));
        if (format == null) {
            throw new IllegalArgumentException("Unknown barcode format: " + id);
        }
        return format;
    }

    public boolean contains(String id) {
        return formats.containsKey(normalize(id));
    }

    public Set<String> ids() {
        return Set.copyOf(formats.keySet());
    }

    public Collection<BarcodeFormat> formats() {
        return formats.values();
    }

    private static String normalize(String id) {
        return id.trim().toUpperCase();
    }

    static FormatSpec linear(String id, String name, boolean checkDigit, boolean text, boolean wideRatio) {
        return new FormatSpec(id, name, SymbolKind.LINEAR, checkDigit, text, wideRatio);
    }

    static FormatSpec matrix(String id, String name) {
        return new FormatSpec(id, name, SymbolKind.MATRIX, false, false, false);
    }

    static BarcodeFormat qrCode() {
        return new MatrixBarcodeFormat(new QRCodeEncoder(QrEccLevel.M), new MatrixRenderer(),
                matrix("QRCODE", "QRCode"));
    }

    static BarcodeFormat dataMatrix() {
        return new MatrixBarcodeFormat(new DataMatrixEncoder(), new MatrixRenderer(),
                matrix("DATAMATRIX", "Data Matrix"));
    }

    static BarcodeFormat pdf417() {
        return new MatrixBarcodeFormat(new PDF417Encoder(), new MatrixRenderer(),
                matrix("PDF417", "PDF417"));
    }

    static BarcodeFormat aztec() {
        return new MatrixBarcodeFormat(new AztecEncoder(), new MatrixRenderer(),
                matrix("AZTEC", "Aztec"));
    }
}
