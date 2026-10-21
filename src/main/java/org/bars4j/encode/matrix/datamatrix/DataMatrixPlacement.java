package org.bars4j.encode.matrix.datamatrix;

import java.util.Arrays;

/**
 * Module placement for ECC200 symbols (ISO/IEC 16022 Annex M).
 *
 * <p>{@link #place()} distributes the codeword bits across the grid that covers the symbol's data
 * regions. The traversal alternates between two diagonal sweeps and emits, at each landing point,
 * an eight-module block; when a sweep leaves the grid it wraps to the opposite edge following the
 * rules of Annex M. Four special corner shapes complete the coverage where a sweep meets the
 * symbol border. The finder patterns are not drawn here: {@link DataMatrixSymbolAssembler} adds
 * them afterwards.
 *
 * <p>The resulting grid exposes {@link #getBit(int, int)}, which the assembler reads back with
 * symbol coordinates local to the data regions.
 */
public final class DataMatrixPlacement {

    private static final byte UNFILLED = -1;
    private static final byte LIGHT = 0;
    private static final byte DARK = 1;

    private final int[] codewords;
    private final int columns;
    private final int rows;
    private final byte[][] grid;

    /**
     * @param codewords the full codeword sequence (data followed by error codewords)
     * @param numCols   width of the data-region grid in modules
     * @param numRows   height of the data-region grid in modules
     */
    public DataMatrixPlacement(int[] codewords, int numCols, int numRows) {
        this.codewords = codewords;
        this.columns = numCols;
        this.rows = numRows;
        this.grid = new byte[numRows][numCols];
        for (byte[] line : grid) {
            Arrays.fill(line, UNFILLED);
        }
    }

    /** Returns whether the module at {@code (col, row)} is dark. */
    public boolean getBit(int col, int row) {
        return grid[row][col] == DARK;
    }

    /** Runs the Annex M.1 placement over the whole data-region grid. */
    public void place() {
        int codeword = 0;
        int row = 4;
        int column = 0;

        do {
            if (row == rows && column == 0) {
                writeCornerAlpha(codeword++);
            }
            if (row == rows - 2 && column == 0 && columns % 4 != 0) {
                writeCornerBeta(codeword++);
            }
            if (row == rows - 2 && column == 0 && columns % 8 == 4) {
                writeCornerGamma(codeword++);
            }
            if (row == rows + 4 && column == 2 && columns % 8 == 0) {
                writeCornerDelta(codeword++);
            }

            // Upward-right sweep.
            do {
                if (row < rows && column >= 0 && isUnfilled(column, row)) {
                    writeBlock(row, column, codeword++);
                }
                row -= 2;
                column += 2;
            } while (row >= 0 && column < columns);

            row += 1;
            column += 3;

            // Downward-left sweep.
            do {
                if (row >= 0 && column < columns && isUnfilled(column, row)) {
                    writeBlock(row, column, codeword++);
                }
                row += 2;
                column -= 2;
            } while (row < rows && column >= 0);

            row += 3;
            column += 1;
        } while (row < rows || column < columns);

        // The bottom-right module pair is always filled to satisfy the symbol's dark corner.
        if (isUnfilled(columns - 1, rows - 1)) {
            write(columns - 1, rows - 1, true);
            write(columns - 2, rows - 2, true);
        }
    }

    /**
     * Writes the eight modules of one codeword block, whose anchor is the block's lower-right
     * corner. The shape is the standard fixed block arrangement of Annex M.1.
     */
    private void writeBlock(int anchorRow, int anchorColumn, int codeword) {
        writeModule(anchorRow - 2, anchorColumn - 2, codeword, 1);
        writeModule(anchorRow - 2, anchorColumn - 1, codeword, 2);
        writeModule(anchorRow - 1, anchorColumn - 2, codeword, 3);
        writeModule(anchorRow - 1, anchorColumn - 1, codeword, 4);
        writeModule(anchorRow - 1, anchorColumn, codeword, 5);
        writeModule(anchorRow, anchorColumn - 2, codeword, 6);
        writeModule(anchorRow, anchorColumn - 1, codeword, 7);
        writeModule(anchorRow, anchorColumn, codeword, 8);
    }

    /**
     * Writes one bit of {@code codeword} at the requested grid position. Coordinates outside the
     * grid are wrapped back inside using the offsets prescribed by Annex M.1.
     */
    private void writeModule(int row, int column, int codeword, int bit) {
        if (row < 0) {
            row += rows;
            column += 4 - ((rows + 4) % 8);
        }
        if (column < 0) {
            column += columns;
            row += 4 - ((columns + 4) % 8);
        }
        boolean dark = (codewords[codeword] & (1 << (8 - bit))) != 0;
        write(column, row, dark);
    }

    private void writeCornerAlpha(int codeword) {
        writeModule(rows - 1, 0, codeword, 1);
        writeModule(rows - 1, 1, codeword, 2);
        writeModule(rows - 1, 2, codeword, 3);
        writeModule(0, columns - 2, codeword, 4);
        writeModule(0, columns - 1, codeword, 5);
        writeModule(1, columns - 1, codeword, 6);
        writeModule(2, columns - 1, codeword, 7);
        writeModule(3, columns - 1, codeword, 8);
    }

    private void writeCornerBeta(int codeword) {
        writeModule(rows - 3, 0, codeword, 1);
        writeModule(rows - 2, 0, codeword, 2);
        writeModule(rows - 1, 0, codeword, 3);
        writeModule(0, columns - 4, codeword, 4);
        writeModule(0, columns - 3, codeword, 5);
        writeModule(0, columns - 2, codeword, 6);
        writeModule(0, columns - 1, codeword, 7);
        writeModule(1, columns - 1, codeword, 8);
    }

    private void writeCornerGamma(int codeword) {
        writeModule(rows - 3, 0, codeword, 1);
        writeModule(rows - 2, 0, codeword, 2);
        writeModule(rows - 1, 0, codeword, 3);
        writeModule(0, columns - 2, codeword, 4);
        writeModule(0, columns - 1, codeword, 5);
        writeModule(1, columns - 1, codeword, 6);
        writeModule(2, columns - 1, codeword, 7);
        writeModule(3, columns - 1, codeword, 8);
    }

    private void writeCornerDelta(int codeword) {
        writeModule(rows - 1, 0, codeword, 1);
        writeModule(rows - 1, columns - 1, codeword, 2);
        writeModule(0, columns - 3, codeword, 3);
        writeModule(0, columns - 2, codeword, 4);
        writeModule(0, columns - 1, codeword, 5);
        writeModule(1, columns - 3, codeword, 6);
        writeModule(1, columns - 2, codeword, 7);
        writeModule(1, columns - 1, codeword, 8);
    }

    private boolean isUnfilled(int column, int row) {
        return grid[row][column] == UNFILLED;
    }

    private void write(int column, int row, boolean dark) {
        grid[row][column] = dark ? DARK : LIGHT;
    }
}
