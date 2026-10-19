package org.bars4j.encode.matrix.datamatrix;

import java.util.Arrays;

/**
 * Symbol character placement for Data Matrix ECC200, adapted from ISO/IEC 16022 Annex M.1.
 *
 * <p>The result is a bit grid of {@code numCols x numRows} modules covering the symbol's data
 * regions only; the finder patterns are added later by the symbol assembler.
 */
public final class DataMatrixPlacement {

    private final int[] codewords;
    private final int numRows;
    private final int numCols;
    private final byte[] bits;

    public DataMatrixPlacement(int[] codewords, int numCols, int numRows) {
        this.codewords = codewords;
        this.numCols = numCols;
        this.numRows = numRows;
        this.bits = new byte[numCols * numRows];
        Arrays.fill(this.bits, (byte) -1);
    }

    public boolean getBit(int col, int row) {
        return bits[row * numCols + col] == 1;
    }

    private void setBit(int col, int row, boolean bit) {
        bits[row * numCols + col] = (byte) (bit ? 1 : 0);
    }

    private boolean noBit(int col, int row) {
        return bits[row * numCols + col] < 0;
    }

    public void place() {
        int pos = 0;
        int row = 4;
        int col = 0;

        do {
            if (row == numRows && col == 0) {
                corner1(pos++);
            }
            if (row == numRows - 2 && col == 0 && numCols % 4 != 0) {
                corner2(pos++);
            }
            if (row == numRows - 2 && col == 0 && numCols % 8 == 4) {
                corner3(pos++);
            }
            if (row == numRows + 4 && col == 2 && numCols % 8 == 0) {
                corner4(pos++);
            }
            do {
                if (row < numRows && col >= 0 && noBit(col, row)) {
                    utah(row, col, pos++);
                }
                row -= 2;
                col += 2;
            } while (row >= 0 && col < numCols);
            row++;
            col += 3;

            do {
                if (row >= 0 && col < numCols && noBit(col, row)) {
                    utah(row, col, pos++);
                }
                row += 2;
                col -= 2;
            } while (row < numRows && col >= 0);
            row += 3;
            col++;

        } while (row < numRows || col < numCols);

        if (noBit(numCols - 1, numRows - 1)) {
            setBit(numCols - 1, numRows - 1, true);
            setBit(numCols - 2, numRows - 2, true);
        }
    }

    private void module(int row, int col, int pos, int bit) {
        if (row < 0) {
            row += numRows;
            col += 4 - ((numRows + 4) % 8);
        }
        if (col < 0) {
            col += numCols;
            row += 4 - ((numCols + 4) % 8);
        }
        int v = codewords[pos] & (1 << (8 - bit));
        setBit(col, row, v != 0);
    }

    private void utah(int row, int col, int pos) {
        module(row - 2, col - 2, pos, 1);
        module(row - 2, col - 1, pos, 2);
        module(row - 1, col - 2, pos, 3);
        module(row - 1, col - 1, pos, 4);
        module(row - 1, col, pos, 5);
        module(row, col - 2, pos, 6);
        module(row, col - 1, pos, 7);
        module(row, col, pos, 8);
    }

    private void corner1(int pos) {
        module(numRows - 1, 0, pos, 1);
        module(numRows - 1, 1, pos, 2);
        module(numRows - 1, 2, pos, 3);
        module(0, numCols - 2, pos, 4);
        module(0, numCols - 1, pos, 5);
        module(1, numCols - 1, pos, 6);
        module(2, numCols - 1, pos, 7);
        module(3, numCols - 1, pos, 8);
    }

    private void corner2(int pos) {
        module(numRows - 3, 0, pos, 1);
        module(numRows - 2, 0, pos, 2);
        module(numRows - 1, 0, pos, 3);
        module(0, numCols - 4, pos, 4);
        module(0, numCols - 3, pos, 5);
        module(0, numCols - 2, pos, 6);
        module(0, numCols - 1, pos, 7);
        module(1, numCols - 1, pos, 8);
    }

    private void corner3(int pos) {
        module(numRows - 3, 0, pos, 1);
        module(numRows - 2, 0, pos, 2);
        module(numRows - 1, 0, pos, 3);
        module(0, numCols - 2, pos, 4);
        module(0, numCols - 1, pos, 5);
        module(1, numCols - 1, pos, 6);
        module(2, numCols - 1, pos, 7);
        module(3, numCols - 1, pos, 8);
    }

    private void corner4(int pos) {
        module(numRows - 1, 0, pos, 1);
        module(numRows - 1, numCols - 1, pos, 2);
        module(0, numCols - 3, pos, 3);
        module(0, numCols - 2, pos, 4);
        module(0, numCols - 1, pos, 5);
        module(1, numCols - 3, pos, 6);
        module(1, numCols - 2, pos, 7);
        module(1, numCols - 1, pos, 8);
    }
}
