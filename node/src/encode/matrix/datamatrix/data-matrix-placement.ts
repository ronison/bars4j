/**
 * Module placement for ECC200 symbols (ISO/IEC 16022 Annex M).
 *
 * <p>{@link DataMatrixPlacement.place} distributes the codeword bits across the grid that covers
 * the symbol's data regions. The traversal alternates between two diagonal sweeps and emits, at
 * each landing point, an eight-module block; when a sweep leaves the grid it wraps to the opposite
 * edge following the rules of Annex M. Four special corner shapes complete the coverage where a
 * sweep meets the symbol border. The finder patterns are not drawn here:
 * {@link DataMatrixSymbolAssembler} adds them afterwards.
 *
 * <p>The resulting grid exposes {@link DataMatrixPlacement.getBit}, which the assembler reads back
 * with symbol coordinates local to the data regions.
 *
 * Port of {@code org.bars4j.encode.matrix.datamatrix.DataMatrixPlacement}.
 */

const UNFILLED = -1;
const LIGHT = 0;
const DARK = 1;

export class DataMatrixPlacement {
  private readonly codewords: number[];
  private readonly columns: number;
  private readonly rows: number;
  private readonly grid: number[][];

  /**
   * @param codewords the full codeword sequence (data followed by error codewords)
   * @param numCols   width of the data-region grid in modules
   * @param numRows   height of the data-region grid in modules
   */
  constructor(codewords: number[], numCols: number, numRows: number) {
    this.codewords = codewords;
    this.columns = numCols;
    this.rows = numRows;
    this.grid = new Array<number[]>(numRows);
    for (let row = 0; row < numRows; row++) {
      this.grid[row] = new Array<number>(numCols).fill(UNFILLED);
    }
  }

  /** Returns whether the module at {@code (col, row)} is dark. */
  getBit(col: number, row: number): boolean {
    return this.grid[row][col] === DARK;
  }

  /** Runs the Annex M.1 placement over the whole data-region grid. */
  place(): void {
    const rows = this.rows;
    const columns = this.columns;
    let codeword = 0;
    let row = 4;
    let column = 0;

    do {
      if (row === rows && column === 0) {
        this.writeCornerAlpha(codeword++);
      }
      if (row === rows - 2 && column === 0 && columns % 4 !== 0) {
        this.writeCornerBeta(codeword++);
      }
      if (row === rows - 2 && column === 0 && columns % 8 === 4) {
        this.writeCornerGamma(codeword++);
      }
      if (row === rows + 4 && column === 2 && columns % 8 === 0) {
        this.writeCornerDelta(codeword++);
      }

      // Upward-right sweep.
      do {
        if (row < rows && column >= 0 && this.isUnfilled(column, row)) {
          this.writeBlock(row, column, codeword++);
        }
        row -= 2;
        column += 2;
      } while (row >= 0 && column < columns);

      row += 1;
      column += 3;

      // Downward-left sweep.
      do {
        if (row >= 0 && column < columns && this.isUnfilled(column, row)) {
          this.writeBlock(row, column, codeword++);
        }
        row += 2;
        column -= 2;
      } while (row < rows && column >= 0);

      row += 3;
      column += 1;
    } while (row < rows || column < columns);

    // The bottom-right module pair is always filled to satisfy the symbol's dark corner.
    if (this.isUnfilled(columns - 1, rows - 1)) {
      this.write(columns - 1, rows - 1, true);
      this.write(columns - 2, rows - 2, true);
    }
  }

  /**
   * Writes the eight modules of one codeword block, whose anchor is the block's lower-right
   * corner. The shape is the standard fixed block arrangement of Annex M.1.
   */
  private writeBlock(anchorRow: number, anchorColumn: number, codeword: number): void {
    this.writeModule(anchorRow - 2, anchorColumn - 2, codeword, 1);
    this.writeModule(anchorRow - 2, anchorColumn - 1, codeword, 2);
    this.writeModule(anchorRow - 1, anchorColumn - 2, codeword, 3);
    this.writeModule(anchorRow - 1, anchorColumn - 1, codeword, 4);
    this.writeModule(anchorRow - 1, anchorColumn, codeword, 5);
    this.writeModule(anchorRow, anchorColumn - 2, codeword, 6);
    this.writeModule(anchorRow, anchorColumn - 1, codeword, 7);
    this.writeModule(anchorRow, anchorColumn, codeword, 8);
  }

  /**
   * Writes one bit of {@code codeword} at the requested grid position. Coordinates outside the
   * grid are wrapped back inside using the offsets prescribed by Annex M.1.
   */
  private writeModule(row: number, column: number, codeword: number, bit: number): void {
    if (row < 0) {
      row += this.rows;
      column += 4 - ((this.rows + 4) % 8);
    }
    if (column < 0) {
      column += this.columns;
      row += 4 - ((this.columns + 4) % 8);
    }
    const dark = (this.codewords[codeword] & (1 << (8 - bit))) !== 0;
    this.write(column, row, dark);
  }

  private writeCornerAlpha(codeword: number): void {
    this.writeModule(this.rows - 1, 0, codeword, 1);
    this.writeModule(this.rows - 1, 1, codeword, 2);
    this.writeModule(this.rows - 1, 2, codeword, 3);
    this.writeModule(0, this.columns - 2, codeword, 4);
    this.writeModule(0, this.columns - 1, codeword, 5);
    this.writeModule(1, this.columns - 1, codeword, 6);
    this.writeModule(2, this.columns - 1, codeword, 7);
    this.writeModule(3, this.columns - 1, codeword, 8);
  }

  private writeCornerBeta(codeword: number): void {
    this.writeModule(this.rows - 3, 0, codeword, 1);
    this.writeModule(this.rows - 2, 0, codeword, 2);
    this.writeModule(this.rows - 1, 0, codeword, 3);
    this.writeModule(0, this.columns - 4, codeword, 4);
    this.writeModule(0, this.columns - 3, codeword, 5);
    this.writeModule(0, this.columns - 2, codeword, 6);
    this.writeModule(0, this.columns - 1, codeword, 7);
    this.writeModule(1, this.columns - 1, codeword, 8);
  }

  private writeCornerGamma(codeword: number): void {
    this.writeModule(this.rows - 3, 0, codeword, 1);
    this.writeModule(this.rows - 2, 0, codeword, 2);
    this.writeModule(this.rows - 1, 0, codeword, 3);
    this.writeModule(0, this.columns - 2, codeword, 4);
    this.writeModule(0, this.columns - 1, codeword, 5);
    this.writeModule(1, this.columns - 1, codeword, 6);
    this.writeModule(2, this.columns - 1, codeword, 7);
    this.writeModule(3, this.columns - 1, codeword, 8);
  }

  private writeCornerDelta(codeword: number): void {
    this.writeModule(this.rows - 1, 0, codeword, 1);
    this.writeModule(this.rows - 1, this.columns - 1, codeword, 2);
    this.writeModule(0, this.columns - 3, codeword, 3);
    this.writeModule(0, this.columns - 2, codeword, 4);
    this.writeModule(0, this.columns - 1, codeword, 5);
    this.writeModule(1, this.columns - 3, codeword, 6);
    this.writeModule(1, this.columns - 2, codeword, 7);
    this.writeModule(1, this.columns - 1, codeword, 8);
  }

  private isUnfilled(column: number, row: number): boolean {
    return this.grid[row][column] === UNFILLED;
  }

  private write(column: number, row: number, dark: boolean): void {
    this.grid[row][column] = dark ? DARK : LIGHT;
  }
}
