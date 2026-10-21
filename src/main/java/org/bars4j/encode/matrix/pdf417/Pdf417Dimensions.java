package org.bars4j.encode.matrix.pdf417;

/**
 * Allowed ranges for the number of columns and rows of a PDF417 symbol.
 *
 * <p>The encoder picks, among the allowed shapes, the one whose width-to-height ratio is closest
 * to the preferred ratio. The default range is 2-30 columns and 2-30 rows.
 *
 * @param minCols smallest number of data columns
 * @param maxCols largest number of data columns
 * @param minRows smallest number of rows
 * @param maxRows largest number of rows
 */
public record Pdf417Dimensions(int minCols, int maxCols, int minRows, int maxRows) {

    public Pdf417Dimensions {
        if (minCols < 1 || maxCols < minCols || minRows < 1 || maxRows < minRows) {
            throw new IllegalArgumentException("invalid PDF417 dimensions");
        }
    }

    /** Returns the standard 2-30 by 2-30 dimensions range. */
    public static Pdf417Dimensions standard() {
        return new Pdf417Dimensions(2, 30, 2, 30);
    }
}
