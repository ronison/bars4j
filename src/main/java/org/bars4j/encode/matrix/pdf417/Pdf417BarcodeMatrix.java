package org.bars4j.encode.matrix.pdf417;

/**
 * The low-level module grid of a PDF417 symbol, built row by row.
 *
 * <p>Rows are stored top-to-bottom (row 0 is the top row). Every row is allocated with room for the
 * start pattern, the two row indicators, the data codewords and the stop pattern; in compact mode
 * the unused trailing modules stay light.
 */
public final class Pdf417BarcodeMatrix {

    private final Pdf417BarcodeRow[] rows;
    private final int widthInModules;
    private int currentRow = -1;

    /**
     * @param height number of rows (PDF417 symbol rows, not modules)
     * @param cols   number of codewords per row (data region width)
     */
    Pdf417BarcodeMatrix(int height, int cols) {
        this.rows = new Pdf417BarcodeRow[height];
        this.widthInModules = (cols + 4) * 17 + 1;
        for (int i = 0; i < height; i++) {
            rows[i] = new Pdf417BarcodeRow(widthInModules);
        }
    }

    /** Advances to the next row to be filled. */
    void startRow() {
        currentRow++;
    }

    /** Returns the row currently being filled. */
    Pdf417BarcodeRow currentRow() {
        return rows[currentRow];
    }

    /** Number of modules in each row. */
    int widthInModules() {
        return widthInModules;
    }

    /** Number of rows. */
    int height() {
        return rows.length;
    }

    /**
     * Returns the module grid as a dense dark/light array, row 0 being the top row.
     *
     * @return {@code [row][column]}, {@code true} meaning dark
     */
    public boolean[][] toModules() {
        boolean[][] result = new boolean[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            result[i] = rows[i].toModules();
        }
        return result;
    }
}
