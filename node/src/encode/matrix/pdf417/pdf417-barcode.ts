import { UnsupportedTextException } from "../../api/unsupported-text-exception.ts";
import { Pdf417BarcodeMatrix } from "./pdf417-barcode-matrix.ts";
import type { Pdf417BarcodeRow } from "./pdf417-barcode-row.ts";
import { pattern } from "./pdf417-code-words.ts";
import { Pdf417Compaction } from "./pdf417-compaction.ts";
import { standardPdf417Dimensions, type Pdf417Dimensions } from "./pdf417-dimensions.ts";
import { codewordCount, generate } from "./pdf417-error-correction.ts";
import { ISO_8859_1, encodeHighLevel, type Pdf417Charset } from "./pdf417-high-level-encoder.ts";

/**
 * Core PDF417 encoder, turning a message into a grid of modules.
 *
 * <p>The path through the encoder is the one described by ISO/IEC 15438: high-level encoding into
 * codewords, selection of a symbol shape, padding of the data region, Reed-Solomon check codewords
 * and finally the low-level bar patterns. Rendering and external libraries are out of scope.
 *
 * Port of {@code org.bars4j.encode.matrix.pdf417.Pdf417Barcode}.
 */

/** Modules in the row-start pattern, which begins every row. */
const ROW_START_MODULES = 17;
/** Bar pattern beginning every row. */
const ROW_START_PATTERN = 0x1fea8;

/** Modules in the row-stop pattern at the end of a full-width row. */
const ROW_STOP_MODULES = 18;
/** Bar pattern ending every full-width row. */
const ROW_STOP_PATTERN = 0x3fa29;

/** Codeword filling unused positions at the right of the data region. */
const PAD_CODEWORD = 900;

/** Width-to-height ratio a symbol shape is chosen to approximate. */
const TARGET_ASPECT = 3.0;
/** Assumed module width, in millimetres, used by the aspect estimate. */
const MODULE_MM = Math.fround(0.357);
/** Assumed row height, in millimetres, used by the aspect estimate. */
const ROW_MM = 2.0;

/** Upper bound on the total number of codewords a PDF417 symbol can hold. */
const MAX_CODEWORDS = 929;

export class Pdf417Barcode {
  private readonly compaction: Pdf417Compaction;
  private readonly charset: Pdf417Charset;
  private readonly compact: boolean;
  private readonly dimensions: Pdf417Dimensions;

  /**
   * @param compaction high-level compaction scheme
   * @param charset    character set used for Text/Byte compaction
   * @param compact    whether to build the reduced-width compact variant
   * @param dimensions allowed column/row ranges
   */
  constructor(
    compaction: Pdf417Compaction | null,
    charset: Pdf417Charset | null,
    compact: boolean,
    dimensions: Pdf417Dimensions | null,
  ) {
    this.compaction = compaction !== null ? compaction : Pdf417Compaction.AUTO;
    this.charset = charset !== null ? charset : ISO_8859_1;
    this.compact = compact;
    this.dimensions = dimensions !== null ? dimensions : standardPdf417Dimensions();
  }

  /**
   * Encodes a message into a grid of modules.
   *
   * @param msg                  the message
   * @param errorCorrectionLevel error correction level, 0-8
   * @return the module grid, row 0 being the top row
   * @throws UnsupportedTextException if the message is empty, contains characters that the
   *                                  character set cannot represent, or does not fit in PDF417
   */
  generateBarcodeLogic(msg: string, errorCorrectionLevel: number): Pdf417BarcodeMatrix {
    const checkCount = codewordCount(errorCorrectionLevel);
    const highLevel = encodeHighLevel(msg, this.compaction, this.charset);
    const dataCount = highLevel.length;

    const shape = this.chooseShape(dataCount, checkCount);
    const cols = shape[0];
    const rows = shape[1];

    if (dataCount + checkCount + 1 > MAX_CODEWORDS) {
      throw new UnsupportedTextException(
        "Encoded message contains too many codewords (" + msg.length + " characters)",
      );
    }

    const pad = Pdf417Barcode.padCount(dataCount, checkCount, cols, rows);
    const data = new Array<number>(dataCount + pad + 1).fill(0);
    data[0] = data.length;
    for (let i = 0; i < dataCount; i++) {
      data[1 + i] = highLevel[i];
    }
    for (let i = 0; i < pad; i++) {
      data[1 + dataCount + i] = PAD_CODEWORD;
    }

    const check = generate(data, errorCorrectionLevel);
    const allCodewords = new Array<number>(data.length + check.length);
    for (let i = 0; i < data.length; i++) {
      allCodewords[i] = data[i];
    }
    for (let i = 0; i < check.length; i++) {
      allCodewords[data.length + i] = check[i];
    }

    const matrix = new Pdf417BarcodeMatrix(rows, cols);
    this.paintRows(allCodewords, cols, rows, errorCorrectionLevel, matrix);
    return matrix;
  }

  /** Rows needed to hold the data and check codewords in the given number of columns. */
  private static rowsFor(dataCount: number, checkCount: number, cols: number): number {
    let rows = Math.trunc((dataCount + 1 + checkCount) / cols) + 1;
    if (cols * rows >= dataCount + 1 + checkCount + cols) {
      rows--;
    }
    return rows;
  }

  /** Number of pad codewords needed to fill the data region exactly. */
  private static padCount(
    dataCount: number,
    checkCount: number,
    cols: number,
    rows: number,
  ): number {
    const fill = cols * rows - checkCount;
    return fill > dataCount + 1 ? fill - dataCount - 1 : 0;
  }

  /**
   * Picks the allowed shape whose estimated aspect ratio is closest to the target ratio.
   */
  private chooseShape(dataCount: number, checkCount: number): number[] {
    let best: number[] | null = null;
    let bestAspect = 0.0;
    for (let cols = this.dimensions.minCols; cols <= this.dimensions.maxCols; cols++) {
      const rows = Pdf417Barcode.rowsFor(dataCount, checkCount, cols);
      if (rows < this.dimensions.minRows) {
        break;
      }
      if (rows > this.dimensions.maxRows) {
        continue;
      }
      // Full-width rows hold one 17-module symbol character per data column plus the
      // start pattern, the two indicators and the 18-module stop (17 * cols + 69).
      const rowModules = ROW_START_MODULES * (cols + 4) + 1;
      const aspect = Math.fround(
        Math.fround(Math.fround(rowModules) * MODULE_MM) / Math.fround(rows * ROW_MM),
      );
      if (
        best !== null &&
        Math.abs(Math.fround(aspect - TARGET_ASPECT)) >
          Math.abs(Math.fround(bestAspect - TARGET_ASPECT))
      ) {
        continue;
      }
      bestAspect = aspect;
      best = [cols, rows];
    }
    if (best === null) {
      const rows = Pdf417Barcode.rowsFor(dataCount, checkCount, this.dimensions.minCols);
      if (rows < this.dimensions.minRows) {
        best = [this.dimensions.minCols, this.dimensions.minRows];
      }
    }
    if (best === null) {
      throw new UnsupportedTextException("Unable to fit message in columns");
    }
    return best;
  }

  /**
   * Appends a bar pattern to a row as alternating runs of dark and light modules.
   *
   * @param pattern the pattern, most significant bit first
   * @param length  number of significant bits in the pattern
   * @param row     the row being filled
   */
  private static appendPattern(pattern: number, length: number, row: Pdf417BarcodeRow): void {
    let bit = 1 << (length - 1);
    let previousDark = (pattern & bit) !== 0;
    let run = 0;
    for (let i = 0; i < length; i++) {
      const dark = (pattern & bit) !== 0;
      if (dark === previousDark) {
        run++;
      } else {
        row.addRun(previousDark, run);
        previousDark = dark;
        run = 1;
      }
      bit >>= 1;
    }
    row.addRun(previousDark, run);
  }

  /** Fills every row: start pattern, row indicators, data codewords and the stop pattern. */
  private paintRows(
    codewords: number[],
    cols: number,
    rows: number,
    ecLevel: number,
    matrix: Pdf417BarcodeMatrix,
  ): void {
    let index = 0;
    for (let rowIndex = 0; rowIndex < rows; rowIndex++) {
      const cluster = rowIndex % 3;
      matrix.startRow();
      const row = matrix.currentRow();
      Pdf417Barcode.appendPattern(ROW_START_PATTERN, ROW_START_MODULES, row);

      let leftIndicator: number;
      let rightIndicator: number;
      const block = 30 * Math.trunc(rowIndex / 3);
      switch (cluster) {
        case 0:
          leftIndicator = block + Math.trunc((rows - 1) / 3);
          rightIndicator = block + (cols - 1);
          break;
        case 1:
          leftIndicator = block + ecLevel * 3 + ((rows - 1) % 3);
          rightIndicator = block + Math.trunc((rows - 1) / 3);
          break;
        default:
          leftIndicator = block + (cols - 1);
          rightIndicator = block + ecLevel * 3 + ((rows - 1) % 3);
          break;
      }

      Pdf417Barcode.appendPattern(pattern(cluster, leftIndicator), ROW_START_MODULES, row);

      for (let col = 0; col < cols; col++) {
        Pdf417Barcode.appendPattern(pattern(cluster, codewords[index]), ROW_START_MODULES, row);
        index++;
      }

      if (this.compact) {
        Pdf417Barcode.appendPattern(ROW_STOP_PATTERN, 1, row);
      } else {
        Pdf417Barcode.appendPattern(pattern(cluster, rightIndicator), ROW_START_MODULES, row);
        Pdf417Barcode.appendPattern(ROW_STOP_PATTERN, ROW_STOP_MODULES, row);
      }
    }
  }
}
