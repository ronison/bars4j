import type { BarcodeSymbol } from "../../symbol/barcode-symbol.ts";
import type { LinearSymbol } from "../../symbol/linear-symbol.ts";

/**
 * Encodes text into a {@link BarcodeSymbol}.
 *
 * Port of {@code org.bars4j.encode.api.BarcodeEncoder}.
 */
export interface BarcodeEncoder<S extends BarcodeSymbol> {
  encode(text: string): S;

  /**
   * Computes the check digit/sum for the given text. Formats without an external check value
   * (e.g. Code128 computes it internally) may return an empty string.
   */
  computeCheckSum(text: string): string;
}

/**
 * Encoder of linear (1D) symbols.
 *
 * Port of {@code org.bars4j.encode.api.LinearEncoder}.
 */
export interface LinearEncoder extends BarcodeEncoder<LinearSymbol> {
  /**
   * Length of a complete symbol including its external check digit, or {@code undefined}/-1 when the
   * format is variable length or has no external check value.
   */
  completeLength?(): number;
}

/** Effective complete length of a linear encoder (mirrors the Java default of -1). */
export function completeLength(encoder: LinearEncoder): number {
  return encoder.completeLength ? encoder.completeLength() : -1;
}
