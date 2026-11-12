package org.bars4j.format;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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

    /**
     * Registers a custom format. Built-in format ids cannot be shadowed: replacing, say,
     * {@code QRCODE} with a different implementation would silently change the behaviour of every
     * caller that looks the format up by id, so re-registering an already known id is rejected.
     *
     * @throws IllegalArgumentException if the id is already registered
     */
    public synchronized void register(BarcodeFormat format) {
        String key = normalize(format.id());
        if (formats.containsKey(key)) {
            throw new IllegalArgumentException("Cannot replace registered format: " + format.id());
        }
        formats.put(key, format);
    }

    public synchronized BarcodeFormat get(String id) {
        BarcodeFormat format = formats.get(normalize(id));
        if (format == null) {
            throw new IllegalArgumentException("Unknown barcode format: " + id);
        }
        return format;
    }

    public synchronized boolean contains(String id) {
        return formats.containsKey(normalize(id));
    }

    public synchronized Set<String> ids() {
        return Set.copyOf(formats.keySet());
    }

    public synchronized Collection<BarcodeFormat> formats() {
        return List.copyOf(formats.values());
    }

    private static String normalize(String id) {
        // Locale.ROOT: the default locale's rules would break the lookup on, for instance,
        // Turkish systems (i -> İ).
        return id.trim().toUpperCase(Locale.ROOT);
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
