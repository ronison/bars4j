/**
 * Geometry and capacity of the Data Matrix ECC200 symbols (ISO/IEC 16022 Table 7).
 *
 * <p>Each entry records the number of data and error codewords, the dimensions of one data region,
 * how many regions tile the symbol and how the codewords are grouped into Reed-Solomon blocks.
 * Symbols are listed in ascending data capacity so that {@link DataMatrixSymbolInfo.lookup} can
 * return the smallest symbol that fits a message.
 *
 * Port of {@code org.bars4j.encode.matrix.datamatrix.DataMatrixSymbolInfo}.
 */

/** Requested symbol shape. */
export const DataMatrixShape = {
  NONE: "NONE",
  SQUARE: "SQUARE",
  RECTANGLE: "RECTANGLE",
} as const;

export type DataMatrixShape = (typeof DataMatrixShape)[keyof typeof DataMatrixShape];

/** One row of the symbol catalogue. */
interface Layout {
  rectangular: boolean;
  dataCodewords: number;
  eccCodewords: number;
  regionColumns: number;
  regionRows: number;
  regionsAcross: number;
  regionsDown: number;
  blockDataCodewords: number;
  blockEccCodewords: number;
}

function layout(
  rectangular: boolean,
  dataCodewords: number,
  eccCodewords: number,
  regionColumns: number,
  regionRows: number,
  regionsAcross: number,
  regionsDown: number,
  blockDataCodewords: number,
  blockEccCodewords: number,
): Layout {
  return {
    rectangular,
    dataCodewords,
    eccCodewords,
    regionColumns,
    regionRows,
    regionsAcross,
    regionsDown,
    blockDataCodewords,
    blockEccCodewords,
  };
}

const LAYOUTS: Layout[] = [
  layout(false, 3, 5, 8, 8, 1, 1, 3, 5),
  layout(false, 5, 7, 10, 10, 1, 1, 5, 7),
  layout(true, 5, 7, 16, 6, 1, 1, 5, 7),
  layout(false, 8, 10, 12, 12, 1, 1, 8, 10),
  layout(true, 10, 11, 14, 6, 2, 1, 10, 11),
  layout(false, 12, 12, 14, 14, 1, 1, 12, 12),
  layout(true, 16, 14, 24, 10, 1, 1, 16, 14),

  layout(false, 18, 14, 16, 16, 1, 1, 18, 14),
  layout(false, 22, 18, 18, 18, 1, 1, 22, 18),
  layout(true, 22, 18, 16, 10, 2, 1, 22, 18),
  layout(false, 30, 20, 20, 20, 1, 1, 30, 20),
  layout(true, 32, 24, 16, 14, 2, 1, 32, 24),
  layout(false, 36, 24, 22, 22, 1, 1, 36, 24),
  layout(false, 44, 28, 24, 24, 1, 1, 44, 28),
  layout(true, 49, 28, 22, 14, 2, 1, 49, 28),

  layout(false, 62, 36, 14, 14, 2, 2, 62, 36),
  layout(false, 86, 42, 16, 16, 2, 2, 86, 42),
  layout(false, 114, 48, 18, 18, 2, 2, 114, 48),
  layout(false, 144, 56, 20, 20, 2, 2, 144, 56),
  layout(false, 174, 68, 22, 22, 2, 2, 174, 68),

  layout(false, 204, 84, 24, 24, 2, 2, 102, 42),
  layout(false, 280, 112, 14, 14, 4, 4, 140, 56),
  layout(false, 368, 144, 16, 16, 4, 4, 92, 36),
  layout(false, 456, 192, 18, 18, 4, 4, 114, 48),
  layout(false, 576, 224, 20, 20, 4, 4, 144, 56),
  layout(false, 696, 272, 22, 22, 4, 4, 174, 68),
  layout(false, 816, 336, 24, 24, 4, 4, 136, 56),
  layout(false, 1050, 408, 18, 18, 6, 6, 175, 68),
  layout(false, 1304, 496, 20, 20, 6, 6, 163, 62),
];

export class DataMatrixSymbolInfo {
  private readonly isRectangular: boolean;
  private readonly dataCodewords: number;
  private readonly eccCodewords: number;
  private readonly regionColumns: number;
  private readonly regionRows: number;
  private readonly regionsAcross: number;
  private readonly regionsDown: number;
  private readonly blockDataCodewords: number;
  private readonly blockEccCodewords: number;

  constructor(
    rectangular: boolean,
    dataCodewords: number,
    eccCodewords: number,
    regionColumns: number,
    regionRows: number,
    regionsAcross: number,
    regionsDown: number,
    blockDataCodewords: number,
    blockEccCodewords: number,
  ) {
    this.isRectangular = rectangular;
    this.dataCodewords = dataCodewords;
    this.eccCodewords = eccCodewords;
    this.regionColumns = regionColumns;
    this.regionRows = regionRows;
    this.regionsAcross = regionsAcross;
    this.regionsDown = regionsDown;
    this.blockDataCodewords = blockDataCodewords;
    this.blockEccCodewords = blockEccCodewords;
  }

  /**
   * Selects the smallest symbol able to hold the given number of data codewords.
   *
   * @param dataCodewords the data codeword count
   * @param shape         shape constraint, or {@link DataMatrixShape.NONE} for any shape
   * @return the matching symbol
   * @throws Error if no symbol is large enough
   */
  static lookup(dataCodewords: number, shape: DataMatrixShape): DataMatrixSymbolInfo {
    for (const symbol of CATALOGUE) {
      if (shape === DataMatrixShape.SQUARE && symbol.rectangular()) {
        continue;
      }
      if (shape === DataMatrixShape.RECTANGLE && !symbol.rectangular()) {
        continue;
      }
      if (dataCodewords <= symbol.dataCodewords) {
        return symbol;
      }
    }
    throw new Error(`No Data Matrix symbol fits ${dataCodewords} data codewords`);
  }

  /** Width of the data-region grid, in modules. */
  symbolDataWidth(): number {
    return this.regionsAcross * this.regionColumns;
  }

  /** Height of the data-region grid, in modules. */
  symbolDataHeight(): number {
    return this.regionsDown * this.regionRows;
  }

  /** Width of the finished symbol, in modules. */
  symbolWidth(): number {
    return this.symbolDataWidth() + this.regionsAcross * 2;
  }

  /** Height of the finished symbol, in modules. */
  symbolHeight(): number {
    return this.symbolDataHeight() + this.regionsDown * 2;
  }

  rectangular(): boolean {
    return this.isRectangular;
  }

  dataCapacity(): number {
    return this.dataCodewords;
  }

  errorCodewords(): number {
    return this.eccCodewords;
  }

  matrixWidth(): number {
    return this.regionColumns;
  }

  matrixHeight(): number {
    return this.regionRows;
  }

  /** Number of Reed-Solomon blocks the data codewords are split into. */
  interleavedBlockCount(): number {
    return Math.trunc(this.dataCodewords / this.blockDataCodewords);
  }

  /** Data codewords in the block with the given one-based index. */
  dataLengthForInterleavedBlock(_index: number): number {
    return this.blockDataCodewords;
  }

  /** Error codewords in the block with the given one-based index. */
  errorLengthForInterleavedBlock(_index: number): number {
    return this.blockEccCodewords;
  }
}

/**
 * The 144x144 ECC200 symbol, whose ten Reed-Solomon blocks are not all the same size.
 *
 * <p>The catalogue assumes uniform blocks, so this symbol overrides the block layout: the first
 * eight blocks carry 156 data codewords and the last two carry 155. Error codewords stay uniform.
 */
class DataMatrixSymbolInfo144 extends DataMatrixSymbolInfo {
  /** Number of data codewords in each of the first eight (larger) blocks. */
  private static readonly LARGE_BLOCK_DATA = 156;

  /** Number of data codewords in each of the two trailing (smaller) blocks. */
  private static readonly SMALL_BLOCK_DATA = 155;

  /** Index up to which blocks are the larger size. */
  private static readonly LARGE_BLOCK_COUNT = 8;

  constructor() {
    super(false, 1558, 620, 22, 22, 6, 6, DataMatrixSymbolInfo144.LARGE_BLOCK_DATA, 62);
  }

  override interleavedBlockCount(): number {
    return 10;
  }

  override dataLengthForInterleavedBlock(index: number): number {
    return index <= DataMatrixSymbolInfo144.LARGE_BLOCK_COUNT
      ? DataMatrixSymbolInfo144.LARGE_BLOCK_DATA
      : DataMatrixSymbolInfo144.SMALL_BLOCK_DATA;
  }
}

function buildCatalogue(): DataMatrixSymbolInfo[] {
  const catalogue: DataMatrixSymbolInfo[] = [];
  for (const l of LAYOUTS) {
    catalogue.push(
      new DataMatrixSymbolInfo(
        l.rectangular,
        l.dataCodewords,
        l.eccCodewords,
        l.regionColumns,
        l.regionRows,
        l.regionsAcross,
        l.regionsDown,
        l.blockDataCodewords,
        l.blockEccCodewords,
      ),
    );
  }
  catalogue.push(new DataMatrixSymbolInfo144());
  return catalogue;
}

const CATALOGUE: DataMatrixSymbolInfo[] = buildCatalogue();
