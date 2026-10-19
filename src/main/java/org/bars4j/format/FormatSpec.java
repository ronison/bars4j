package org.bars4j.format;

import org.bars4j.symbol.SymbolKind;

/**
 * Static metadata describing a barcode format.
 *
 * @param id                unique identifier (e.g. "EAN13", "QRCODE")
 * @param displayName       human readable name
 * @param kind              linear (1D) or matrix (2D)
 * @param supportsCheckDigit whether the format exposes an external check value
 * @param allowsText        whether a human-readable text can be drawn under the symbol
 * @param allowsWideRatio   whether the format uses wide/narrow elements
 */
public record FormatSpec(String id, String displayName, SymbolKind kind,
                         boolean supportsCheckDigit, boolean allowsText, boolean allowsWideRatio) {
}
