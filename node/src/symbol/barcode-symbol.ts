import type { SymbolKind } from "./symbol-kind.ts";

/**
 * Result of the encoding step, independent of how it will be rendered.
 *
 * Port of {@code org.bars4j.symbol.BarcodeSymbol}.
 */
export interface BarcodeSymbol {
  kind(): SymbolKind;

  /** Number of elements for linear symbols, or width in modules for matrix symbols. */
  widthInModules(): number;

  /** 1 for linear symbols, or height in modules for matrix symbols. */
  heightInModules(): number;
}
