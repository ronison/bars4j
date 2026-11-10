import { Pdf417BarcodeRow } from "./pdf417-barcode-row.ts";

/**
 * Module grid of a PDF417 symbol, assembled row by row from the top down.
 *
 * <p>Every row is allocated wide enough for the start pattern, the two row indicators, all data
 * codewords and the stop pattern. In compact mode the modules past the shortened stop sequence stay
 * light, so the allocation is never exceeded.
 *
 * Port of {@code org.bars4j.encode.matrix.pdf417.Pdf417BarcodeMatrix}.
 */
export class Pdf417BarcodeMatrix {
  /** Modules in a single symbol character (codeword or row indicator). */
  private static readonly SYMBOL_CHARACTER_MODULES = 17;
  /** Modules in the stop pattern of a full-width row. */
  private static readonly STOP_MODULES = 18;

  private readonly rows: Pdf417BarcodeRow[];
  private readonly modulesPerRow: number;
  private nextRow = -1;

  /**
   * @param height number of symbol rows
   * @param cols   number of data codewords per row
   */
  constructor(height: number, cols: number) {
    this.rows = new Array<Pdf417BarcodeRow>(height);
    // start pattern + two indicators + data columns + stop pattern
    this.modulesPerRow =
      Pdf417BarcodeMatrix.SYMBOL_CHARACTER_MODULES * (cols + 3) + Pdf417BarcodeMatrix.STOP_MODULES;
    for (let i = 0; i < height; i++) {
      this.rows[i] = new Pdf417BarcodeRow(this.modulesPerRow);
    }
  }

  /** Moves on to the next row to be filled. */
  startRow(): void {
    this.nextRow++;
  }

  /** Returns the row currently being filled. */
  currentRow(): Pdf417BarcodeRow {
    return this.rows[this.nextRow];
  }

  /** Number of modules in every row. */
  widthInModules(): number {
    return this.modulesPerRow;
  }

  /** Number of symbol rows. */
  height(): number {
    return this.rows.length;
  }

  /**
   * Returns the grid as a dense dark/light array, row 0 being the top row.
   *
   * @return {@code [row][column]}, {@code true} meaning dark
   */
  toModules(): boolean[][] {
    const grid: boolean[][] = new Array<boolean[]>(this.rows.length);
    for (let i = 0; i < this.rows.length; i++) {
      grid[i] = this.rows[i].toModules();
    }
    return grid;
  }
}
