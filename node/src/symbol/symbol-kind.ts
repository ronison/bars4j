/**
 * The two structural families of barcode symbols supported by the library.
 *
 * Port of {@code org.bars4j.symbol.SymbolKind}.
 */
export const SymbolKind = {
  /** Linear (1D) barcodes: a sequence of vertical bars. */
  LINEAR: "LINEAR",
  /** Matrix (2D) barcodes: a grid of modules (e.g. QRCode). */
  MATRIX: "MATRIX",
} as const;

export type SymbolKind = (typeof SymbolKind)[keyof typeof SymbolKind];
