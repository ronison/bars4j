package org.bars4j.symbol;

/**
 * The two structural families of barcode symbols supported by the library.
 */
public enum SymbolKind {
    /** Linear (1D) barcodes: a sequence of vertical bars. */
    LINEAR,
    /** Matrix (2D) barcodes: a grid of modules (e.g. QRCode). */
    MATRIX
}
