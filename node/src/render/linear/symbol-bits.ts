import type { LinearSymbol } from "../../symbol/linear-symbol.ts";

/**
 * Extracts the legacy bit array (one boolean per element) from a {@link LinearSymbol}.
 *
 * Port of {@code org.bars4j.render.linear.SymbolBits}.
 */
export function dark(symbol: LinearSymbol): boolean[] {
  return symbol.elements.map((e) => e.dark);
}

export function wide(symbol: LinearSymbol): boolean[] {
  return symbol.elements.map((e) => e.wide);
}
