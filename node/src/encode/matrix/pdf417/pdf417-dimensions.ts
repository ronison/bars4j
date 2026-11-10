/**
 * Allowed ranges for the number of columns and rows of a PDF417 symbol.
 *
 * <p>The encoder picks, among the allowed shapes, the one whose width-to-height ratio is closest
 * to the preferred ratio. The default range is 2-30 columns and 2-30 rows.
 *
 * Port of {@code org.bars4j.encode.matrix.pdf417.Pdf417Dimensions}.
 */
export interface Pdf417Dimensions {
  /** smallest number of data columns */
  readonly minCols: number;
  /** largest number of data columns */
  readonly maxCols: number;
  /** smallest number of rows */
  readonly minRows: number;
  /** largest number of rows */
  readonly maxRows: number;
}

/**
 * Validates and creates a dimensions range.
 *
 * @throws Error when the ranges are invalid
 */
export function pdf417Dimensions(
  minCols: number,
  maxCols: number,
  minRows: number,
  maxRows: number,
): Pdf417Dimensions {
  if (minCols < 1 || maxCols < minCols || minRows < 1 || maxRows < minRows) {
    throw new Error("invalid PDF417 dimensions");
  }
  return { minCols, maxCols, minRows, maxRows };
}

/** Returns the standard 2-30 by 2-30 dimensions range. */
export function standardPdf417Dimensions(): Pdf417Dimensions {
  return pdf417Dimensions(2, 30, 2, 30);
}
