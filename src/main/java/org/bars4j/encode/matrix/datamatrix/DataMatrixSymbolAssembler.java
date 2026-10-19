package org.bars4j.encode.matrix.datamatrix;

/**
 * Assembles the final Data Matrix symbol by surrounding each data region with its finder pattern.
 *
 * <p>Per the ECC200 standard each data region is framed by a solid dark "L" on the left and bottom
 * edges and an alternating light/dark pattern on the top and right edges.
 */
public final class DataMatrixSymbolAssembler {

    private DataMatrixSymbolAssembler() {
    }

    public static boolean[][] assemble(DataMatrixPlacement placement, DataMatrixSymbolInfo symbolInfo) {
        int dataWidth = symbolInfo.symbolDataWidth();
        int dataHeight = symbolInfo.symbolDataHeight();
        int matrixWidth = symbolInfo.matrixWidth();
        int matrixHeight = symbolInfo.matrixHeight();
        int symbolWidth = symbolInfo.symbolWidth();

        boolean[][] matrix = new boolean[symbolInfo.symbolHeight()][symbolWidth];
        int matrixY = 0;

        for (int y = 0; y < dataHeight; y++) {
            if (y % matrixHeight == 0) {
                // Top edge of each data region: alternating modules, starting dark.
                for (int x = 0; x < symbolWidth; x++) {
                    matrix[matrixY][x] = (x % 2) == 0;
                }
                matrixY++;
            }
            int matrixX = 0;
            for (int x = 0; x < dataWidth; x++) {
                if (x % matrixWidth == 0) {
                    matrix[matrixY][matrixX++] = true; // left edge: solid dark
                }
                matrix[matrixY][matrixX++] = placement.getBit(x, y);
                if (x % matrixWidth == matrixWidth - 1) {
                    matrix[matrixY][matrixX++] = (y % 2) == 0; // right edge: alternating
                }
            }
            matrixY++;
            if (y % matrixHeight == matrixHeight - 1) {
                // Bottom edge of each data region: solid dark.
                for (int x = 0; x < symbolWidth; x++) {
                    matrix[matrixY][x] = true;
                }
                matrixY++;
            }
        }
        return matrix;
    }
}
