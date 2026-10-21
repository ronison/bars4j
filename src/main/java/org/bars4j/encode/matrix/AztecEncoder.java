package org.bars4j.encode.matrix;

import java.nio.charset.Charset;

import org.bars4j.encode.api.MatrixEncoder;
import org.bars4j.encode.api.UnsupportedTextException;
import org.bars4j.encode.matrix.aztec.AztecCode;
import org.bars4j.encode.matrix.aztec.AztecSymbolEncoder;
import org.bars4j.symbol.MatrixSymbol;

/**
 * Self-contained Aztec (ISO/IEC 24778) encoder.
 *
 * <p>By default it targets the minimal error correction recommended for Aztec (33%) and picks the
 * smallest symbol that fits, alternating between compact and full layouts. No runtime dependency on
 * any external library is required; the default text encoding is ISO-8859-1 (Latin-1), matching the
 * implicit Aztec character set.
 */
public final class AztecEncoder implements MatrixEncoder {

    /** Default minimum percentage of error correction words. */
    public static final int DEFAULT_ECC_PERCENT = 33;
    /** Quiet zone required by the Aztec standard, in modules. */
    private static final int QUIET_ZONE = 1;

    private final int eccPercent;
    private final Integer forcedLayers;
    private final Charset charset;

    /** Creates an encoder with automatic sizing and no ECI. */
    public AztecEncoder() {
        this(DEFAULT_ECC_PERCENT, null, null);
    }

    /** Creates an encoder with the given error correction percentage and automatic sizing. */
    public AztecEncoder(int eccPercent) {
        this(eccPercent, null, null);
    }

    /**
     * Creates a fully configured encoder.
     *
     * @param eccPercent   minimum percentage of error correction words
     * @param forcedLayers {@code null} or {@code 0} to size automatically; a negative value forces
     *                     a compact symbol with {@code abs(value)} layers, a positive value forces
     *                     a full symbol with that many layers
     * @param charset      character set announced with an ECI, or {@code null} for no ECI (the text
     *                     is then encoded as ISO-8859-1)
     */
    public AztecEncoder(int eccPercent, Integer forcedLayers, Charset charset) {
        this.eccPercent = eccPercent;
        this.forcedLayers = forcedLayers;
        this.charset = charset;
    }

    @Override
    public MatrixSymbol encode(String text) throws UnsupportedTextException {
        AztecCode code = AztecSymbolEncoder.encode(text, eccPercent, forcedLayers, charset);
        return new MatrixSymbol(code.modules(), QUIET_ZONE);
    }

    @Override
    public String computeCheckSum(String text) {
        // Aztec error correction is computed internally; there is no external check value.
        return "";
    }
}
