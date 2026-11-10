import type { BarElement } from "./bar-element.ts";
import type { BarcodeSymbol } from "./barcode-symbol.ts";
import { SymbolKind } from "./symbol-kind.ts";

/**
 * A linear (1D) barcode symbol: an ordered sequence of {@link BarElement}s drawn as vertical bars.
 *
 * Port of {@code org.bars4j.symbol.LinearSymbol}.
 */
export class LinearSymbol implements BarcodeSymbol {
  readonly elements: readonly BarElement[];

  constructor(elements: BarElement[]) {
    this.elements = elements.slice();
  }

  kind(): SymbolKind {
    return SymbolKind.LINEAR;
  }

  widthInModules(): number {
    return this.elements.length;
  }

  heightInModules(): number {
    return 1;
  }
}
