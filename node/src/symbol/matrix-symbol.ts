import type { BarcodeSymbol } from "./barcode-symbol.ts";
import { SymbolKind } from "./symbol-kind.ts";

/**
 * A matrix (2D) barcode symbol: a grid of modules.
 *
 * Port of {@code org.bars4j.symbol.MatrixSymbol}.
 */
export class MatrixSymbol implements BarcodeSymbol {
  private readonly grid: readonly (readonly boolean[])[];
  private readonly quietZoneValue: number;

  /**
   * @param modules   {@code modules[row][column] === true} means a dark module
   * @param quietZone size of the mandatory quiet zone around the symbol, in modules
   */
  constructor(modules: boolean[][], quietZone: number) {
    if (modules.length === 0) {
      throw new Error("modules must not be empty");
    }
    const width = modules[0].length;
    for (const row of modules) {
      if (row.length !== width) {
        throw new Error("modules must be rectangular");
      }
    }
    this.grid = modules.map((row) => row.slice());
    this.quietZoneValue = quietZone;
  }

  /** Returns a defensive copy of the module matrix. */
  modules(): boolean[][] {
    return this.grid.map((row) => row.slice());
  }

  module(row: number, column: number): boolean {
    return this.grid[row][column];
  }

  quietZone(): number {
    return this.quietZoneValue;
  }

  kind(): SymbolKind {
    return SymbolKind.MATRIX;
  }

  widthInModules(): number {
    return this.grid[0].length;
  }

  heightInModules(): number {
    return this.grid.length;
  }
}
