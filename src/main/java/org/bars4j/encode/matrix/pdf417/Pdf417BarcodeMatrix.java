package org.bars4j.encode.matrix.pdf417;

/**
 * Module grid of a PDF417 symbol, assembled row by row from the top down.
 *
 * <p>Every row is allocated wide enough for the start pattern, the two row indicators, all data
 * codewords and the stop pattern. In compact mode the modules past the shortened stop sequence stay
 * light, so the allocation is never exceeded.
 */
public final class Pdf417BarcodeMatrix {

    /** Modules in a single symbol character (codeword or row indicator). */
    private static final int SYMBOL_CHARACTER_MODULES = 17;
    /** Modules in the stop pattern of a full-width row. */
    private static final int STOP_MODULES = 18;

    private final Pdf417BarcodeRow[] rows;
    private final int modulesPerRow;
    private int nextRow = -1;

    /**
     * @param height number of symbol rows
     * @param cols   number of data codewords per row
     */
    Pdf417BarcodeMatrix(int height, int cols) {
        this.rows = new Pdf417BarcodeRow[height];
        // start pattern + two indicators + data columns + stop pattern
        this.modulesPerRow = SYMBOL_CHARACTER_MODULES * (cols + 3) + STOP_MODULES;
        for (int i = 0; i < height; i++) {
            rows[i] = new Pdf417BarcodeRow(modulesPerRow);
        }
    }

    /** Moves on to the next row to be filled. */
    void startRow() {
        nextRow++;
    }

    /** Returns the row currently being filled. */
    Pdf417BarcodeRow currentRow() {
        return rows[nextRow];
    }

    /** Number of modules in every row. */
    int widthInModules() {
        return modulesPerRow;
    }

    /** Number of symbol rows. */
    int height() {
        return rows.length;
    }

    /**
     * Returns the grid as a dense dark/light array, row 0 being the top row.
     *
     * @return {@code [row][column]}, {@code true} meaning dark
     */
    public boolean[][] toModules() {
        boolean[][] grid = new boolean[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            grid[i] = rows[i].toModules();
        }
        return grid;
    }
}
