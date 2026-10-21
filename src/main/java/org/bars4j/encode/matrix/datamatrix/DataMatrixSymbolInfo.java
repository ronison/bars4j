package org.bars4j.encode.matrix.datamatrix;

/**
 * Geometry and capacity of the Data Matrix ECC200 symbols (ISO/IEC 16022 Table 7).
 *
 * <p>Each entry records the number of data and error codewords, the dimensions of one data region,
 * how many regions tile the symbol and how the codewords are grouped into Reed-Solomon blocks.
 * Symbols are listed in ascending data capacity so that {@link #lookup} can return the smallest
 * symbol that fits a message.
 */
public class DataMatrixSymbolInfo {

    /** Requested symbol shape. */
    public enum Shape {
        NONE, SQUARE, RECTANGLE
    }

    /**
     * One row of the symbol catalogue.
     *
     * @param rectangular        whether the symbol is rectangular
     * @param dataCodewords      number of data codewords at capacity
     * @param eccCodewords       number of error codewords
     * @param regionColumns      data-region width in modules
     * @param regionRows         data-region height in modules
     * @param regionsAcross      number of data regions per symbol row
     * @param regionsDown        number of data regions per symbol column
     * @param blockDataCodewords data codewords per Reed-Solomon block
     * @param blockEccCodewords  error codewords per Reed-Solomon block
     */
    private record Layout(boolean rectangular, int dataCodewords, int eccCodewords,
                          int regionColumns, int regionRows, int regionsAcross, int regionsDown,
                          int blockDataCodewords, int blockEccCodewords) {
    }

    private static final Layout[] LAYOUTS = {
            new Layout(false, 3, 5, 8, 8, 1, 1, 3, 5),
            new Layout(false, 5, 7, 10, 10, 1, 1, 5, 7),
            new Layout(true, 5, 7, 16, 6, 1, 1, 5, 7),
            new Layout(false, 8, 10, 12, 12, 1, 1, 8, 10),
            new Layout(true, 10, 11, 14, 6, 2, 1, 10, 11),
            new Layout(false, 12, 12, 14, 14, 1, 1, 12, 12),
            new Layout(true, 16, 14, 24, 10, 1, 1, 16, 14),

            new Layout(false, 18, 14, 16, 16, 1, 1, 18, 14),
            new Layout(false, 22, 18, 18, 18, 1, 1, 22, 18),
            new Layout(true, 22, 18, 16, 10, 2, 1, 22, 18),
            new Layout(false, 30, 20, 20, 20, 1, 1, 30, 20),
            new Layout(true, 32, 24, 16, 14, 2, 1, 32, 24),
            new Layout(false, 36, 24, 22, 22, 1, 1, 36, 24),
            new Layout(false, 44, 28, 24, 24, 1, 1, 44, 28),
            new Layout(true, 49, 28, 22, 14, 2, 1, 49, 28),

            new Layout(false, 62, 36, 14, 14, 2, 2, 62, 36),
            new Layout(false, 86, 42, 16, 16, 2, 2, 86, 42),
            new Layout(false, 114, 48, 18, 18, 2, 2, 114, 48),
            new Layout(false, 144, 56, 20, 20, 2, 2, 144, 56),
            new Layout(false, 174, 68, 22, 22, 2, 2, 174, 68),

            new Layout(false, 204, 84, 24, 24, 2, 2, 102, 42),
            new Layout(false, 280, 112, 14, 14, 4, 4, 140, 56),
            new Layout(false, 368, 144, 16, 16, 4, 4, 92, 36),
            new Layout(false, 456, 192, 18, 18, 4, 4, 114, 48),
            new Layout(false, 576, 224, 20, 20, 4, 4, 144, 56),
            new Layout(false, 696, 272, 22, 22, 4, 4, 174, 68),
            new Layout(false, 816, 336, 24, 24, 4, 4, 136, 56),
            new Layout(false, 1050, 408, 18, 18, 6, 6, 175, 68),
            new Layout(false, 1304, 496, 20, 20, 6, 6, 163, 62)
    };

    private static final DataMatrixSymbolInfo[] CATALOGUE = buildCatalogue();

    private final boolean rectangular;
    private final int dataCodewords;
    private final int eccCodewords;
    private final int regionColumns;
    private final int regionRows;
    private final int regionsAcross;
    private final int regionsDown;
    private final int blockDataCodewords;
    private final int blockEccCodewords;

    DataMatrixSymbolInfo(boolean rectangular, int dataCodewords, int eccCodewords,
                         int regionColumns, int regionRows, int regionsAcross, int regionsDown,
                         int blockDataCodewords, int blockEccCodewords) {
        this.rectangular = rectangular;
        this.dataCodewords = dataCodewords;
        this.eccCodewords = eccCodewords;
        this.regionColumns = regionColumns;
        this.regionRows = regionRows;
        this.regionsAcross = regionsAcross;
        this.regionsDown = regionsDown;
        this.blockDataCodewords = blockDataCodewords;
        this.blockEccCodewords = blockEccCodewords;
    }

    private static DataMatrixSymbolInfo[] buildCatalogue() {
        DataMatrixSymbolInfo[] catalogue = new DataMatrixSymbolInfo[LAYOUTS.length + 1];
        for (int i = 0; i < LAYOUTS.length; i++) {
            Layout layout = LAYOUTS[i];
            catalogue[i] = new DataMatrixSymbolInfo(
                    layout.rectangular(), layout.dataCodewords(), layout.eccCodewords(),
                    layout.regionColumns(), layout.regionRows(), layout.regionsAcross(),
                    layout.regionsDown(), layout.blockDataCodewords(), layout.blockEccCodewords());
        }
        catalogue[LAYOUTS.length] = new DataMatrixSymbolInfo144();
        return catalogue;
    }

    /**
     * Selects the smallest symbol able to hold the given number of data codewords.
     *
     * @param dataCodewords the data codeword count
     * @param shape         shape constraint, or {@link Shape#NONE} for any shape
     * @return the matching symbol
     * @throws IllegalArgumentException if no symbol is large enough
     */
    public static DataMatrixSymbolInfo lookup(int dataCodewords, Shape shape) {
        for (DataMatrixSymbolInfo symbol : CATALOGUE) {
            if (shape == Shape.SQUARE && symbol.rectangular) {
                continue;
            }
            if (shape == Shape.RECTANGLE && !symbol.rectangular) {
                continue;
            }
            if (dataCodewords <= symbol.dataCodewords) {
                return symbol;
            }
        }
        throw new IllegalArgumentException(
                "No Data Matrix symbol fits " + dataCodewords + " data codewords");
    }

    /** Width of the data-region grid, in modules. */
    public int symbolDataWidth() {
        return regionsAcross * regionColumns;
    }

    /** Height of the data-region grid, in modules. */
    public int symbolDataHeight() {
        return regionsDown * regionRows;
    }

    /** Width of the finished symbol, in modules. */
    public int symbolWidth() {
        return symbolDataWidth() + regionsAcross * 2;
    }

    /** Height of the finished symbol, in modules. */
    public int symbolHeight() {
        return symbolDataHeight() + regionsDown * 2;
    }

    public boolean rectangular() {
        return rectangular;
    }

    public int dataCapacity() {
        return dataCodewords;
    }

    public int errorCodewords() {
        return eccCodewords;
    }

    public int matrixWidth() {
        return regionColumns;
    }

    public int matrixHeight() {
        return regionRows;
    }

    /** Number of Reed-Solomon blocks the data codewords are split into. */
    public int interleavedBlockCount() {
        return dataCodewords / blockDataCodewords;
    }

    /** Data codewords in the block with the given one-based index. */
    public int dataLengthForInterleavedBlock(int index) {
        return blockDataCodewords;
    }

    /** Error codewords in the block with the given one-based index. */
    public int errorLengthForInterleavedBlock(int index) {
        return blockEccCodewords;
    }
}
