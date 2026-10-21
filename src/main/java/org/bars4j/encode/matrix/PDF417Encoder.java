package org.bars4j.encode.matrix;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.bars4j.encode.api.MatrixEncoder;
import org.bars4j.encode.api.UnsupportedTextException;
import org.bars4j.encode.matrix.pdf417.Pdf417Barcode;
import org.bars4j.encode.matrix.pdf417.Pdf417BarcodeMatrix;
import org.bars4j.encode.matrix.pdf417.Pdf417Compaction;
import org.bars4j.encode.matrix.pdf417.Pdf417Dimensions;
import org.bars4j.symbol.MatrixSymbol;

/**
 * Self-contained PDF417 encoder (ISO/IEC 15438).
 *
 * <p>Uses automatic compaction (Text/Byte/Numeric), Reed-Solomon error correction and the standard
 * codeword patterns. No runtime dependency on any external library. The default configuration is
 * error correction level 2, ISO-8859-1 input, non-compact layout and a two-module quiet zone.
 */
public final class PDF417Encoder implements MatrixEncoder {

    /** Quiet zone required on each side of a PDF417 symbol, in modules. */
    private static final int QUIET_ZONE = 2;

    /** Error correction level recommended by the standard for small amounts of data. */
    private static final int DEFAULT_ERROR_CORRECTION_LEVEL = 2;

    private final int errorCorrectionLevel;
    private final Charset charset;
    private final boolean compact;

    /** Creates an encoder with the default configuration. */
    public PDF417Encoder() {
        this(DEFAULT_ERROR_CORRECTION_LEVEL, StandardCharsets.ISO_8859_1, false);
    }

    /**
     * Creates an encoder with the given error correction level and otherwise default settings.
     *
     * @param errorCorrectionLevel error correction level, 0-8
     */
    public PDF417Encoder(int errorCorrectionLevel) {
        this(errorCorrectionLevel, StandardCharsets.ISO_8859_1, false);
    }

    /**
     * Creates a fully configured encoder.
     *
     * @param errorCorrectionLevel error correction level, 0-8
     * @param charset              character set used for the message
     * @param compact              whether to build the compact (reduced-width) variant
     */
    public PDF417Encoder(int errorCorrectionLevel, Charset charset, boolean compact) {
        if (errorCorrectionLevel < 0 || errorCorrectionLevel > 8) {
            throw new IllegalArgumentException("Error correction level must be between 0 and 8");
        }
        this.errorCorrectionLevel = errorCorrectionLevel;
        this.charset = charset != null ? charset : StandardCharsets.ISO_8859_1;
        this.compact = compact;
    }

    /** Returns the configured error correction level. */
    public int errorCorrectionLevel() {
        return errorCorrectionLevel;
    }

    @Override
    public MatrixSymbol encode(String text) throws UnsupportedTextException {
        Pdf417Barcode barcode = new Pdf417Barcode(
                Pdf417Compaction.AUTO, charset, compact, Pdf417Dimensions.standard());
        Pdf417BarcodeMatrix matrix = barcode.generateBarcodeLogic(text, errorCorrectionLevel);
        return new MatrixSymbol(matrix.toModules(), QUIET_ZONE);
    }

    @Override
    public String computeCheckSum(String text) {
        // PDF417 error correction is internal; there is no external check digit.
        return "";
    }
}
