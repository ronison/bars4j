import type { SymbolKind } from "../symbol/symbol-kind.ts";

/**
 * Static metadata describing a barcode format.
 *
 * Port of {@code org.bars4j.format.FormatSpec}.
 */
export interface FormatSpec {
  /** Unique identifier (e.g. "EAN13", "QRCODE"). */
  readonly id: string;
  /** Human readable name. */
  readonly displayName: string;
  /** Linear (1D) or matrix (2D). */
  readonly kind: SymbolKind;
  /** Whether the format exposes an external check value. */
  readonly supportsCheckDigit: boolean;
  /** Whether a human-readable text can be drawn under the symbol. */
  readonly allowsText: boolean;
  /** Whether the format uses wide/narrow elements. */
  readonly allowsWideRatio: boolean;
}
