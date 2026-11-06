package org.bars4j.encode.api;

import org.bars4j.symbol.LinearSymbol;

/** Encoder of linear (1D) symbols. */
public interface LinearEncoder extends BarcodeEncoder<LinearSymbol> {

    /**
     * Length of a complete symbol including its external check digit, or {@code -1} when the
     * format is variable length or has no external check value.
     *
     * <p>Used by the format facade to tell a payload (check digit still to be appended) from a
     * complete code (check digit already supplied and therefore to be validated/kept verbatim).
     */
    default int completeLength() {
        return -1;
    }
}
