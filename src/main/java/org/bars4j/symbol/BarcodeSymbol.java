package org.bars4j.symbol;

/**
 * Result of the encoding step, independent of how it will be rendered.
 *
 * <p>The current pipeline only knows about {@link LinearSymbol}; {@link MatrixSymbol} was
 * introduced to support 2D codes such as QRCode.
 */
public sealed interface BarcodeSymbol permits LinearSymbol, MatrixSymbol {

    SymbolKind kind();

    /** Number of elements for linear symbols, or width in modules for matrix symbols. */
    int widthInModules();

    /** 1 for linear symbols, or height in modules for matrix symbols. */
    int heightInModules();
}
