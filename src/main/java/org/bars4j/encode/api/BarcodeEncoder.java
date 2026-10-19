package org.bars4j.encode.api;

import org.bars4j.symbol.BarcodeSymbol;
import org.bars4j.symbol.LinearSymbol;
import org.bars4j.symbol.MatrixSymbol;

/**
 * Encodes text into a {@link BarcodeSymbol}.
 *
 * @param <S> the concrete symbol type produced by this encoder
 */
public interface BarcodeEncoder<S extends BarcodeSymbol> {

    S encode(String text) throws BarcodeException;

    /**
     * Computes the check digit/sum for the given text. Formats without an external check value
     * (e.g. Code128 computes it internally) may return an empty string.
     */
    String computeCheckSum(String text) throws BarcodeException;
}
