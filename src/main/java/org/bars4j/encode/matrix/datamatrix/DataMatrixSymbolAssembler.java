package org.bars4j.encode.matrix.datamatrix;

/**
 * Draws the complete ECC200 symbol by framing every data region with its finder pattern.
 *
 * <p>An ECC200 symbol is tiled from data regions. Each region is bounded by a solid dark "L" along
 * its left and bottom sides and by an alternating light/dark border along its top and right sides.
 * The assembler visits the regions in row-major order, paints those borders and copies the placed
 * data modules into the interior.
 */
public final class DataMatrixSymbolAssembler {

    private DataMatrixSymbolAssembler() {
    }

    /**
     * Produces the module grid of the finished symbol.
     *
     * @param placement  the placed data regions
     * @param symbolInfo the symbol geometry
     * @return {@code symbolHeight()} rows of {@code symbolWidth()} modules, {@code true} for dark
     */
    public static boolean[][] assemble(DataMatrixPlacement placement, DataMatrixSymbolInfo symbolInfo) {
        int regionRows = symbolInfo.matrixHeight();
        int regionColumns = symbolInfo.matrixWidth();
        int regionsDown = symbolInfo.symbolDataHeight() / regionRows;
        int regionsAcross = symbolInfo.symbolDataWidth() / regionColumns;

        boolean[][] symbol = new boolean[symbolInfo.symbolHeight()][symbolInfo.symbolWidth()];

        for (int tileRow = 0; tileRow < regionsDown; tileRow++) {
            for (int tileColumn = 0; tileColumn < regionsAcross; tileColumn++) {
                drawRegion(symbol, placement, tileRow, tileColumn, regionRows, regionColumns);
            }
        }
        return symbol;
    }

    private static void drawRegion(boolean[][] symbol, DataMatrixPlacement placement,
                                   int tileRow, int tileColumn, int regionRows, int regionColumns) {
        int originRow = tileRow * (regionRows + 2);
        int originColumn = tileColumn * (regionColumns + 2);
        int span = regionColumns + 2;

        // Top border: alternating modules, dark at even offsets.
        for (int offset = 0; offset < span; offset++) {
            symbol[originRow][originColumn + offset] = (offset % 2) == 0;
        }

        for (int localRow = 0; localRow < regionRows; localRow++) {
            int dataRow = tileRow * regionRows + localRow;
            int symbolRow = originRow + 1 + localRow;

            symbol[symbolRow][originColumn] = true; // left border: solid dark
            for (int localColumn = 0; localColumn < regionColumns; localColumn++) {
                int dataColumn = tileColumn * regionColumns + localColumn;
                symbol[symbolRow][originColumn + 1 + localColumn] = placement.getBit(dataColumn, dataRow);
            }
            symbol[symbolRow][originColumn + span - 1] = (dataRow % 2) == 0; // right border
        }

        // Bottom border: solid dark.
        for (int offset = 0; offset < span; offset++) {
            symbol[originRow + regionRows + 1][originColumn + offset] = true;
        }
    }
}
