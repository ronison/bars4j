package org.bars4j.encode.matrix.datamatrix;

/**
 * Symbol metadata table for Data Matrix ECC200.
 *
 * <p>Mirrors the ISO/IEC 16022 symbol table (square and rectangular symbols), including the
 * number of data regions, data/error codewords and the Reed-Solomon interleaving layout.
 */
public class DataMatrixSymbolInfo {

    /** Requested symbol shape. */
    public enum Shape {
        NONE, SQUARE, RECTANGLE
    }

    private static final DataMatrixSymbolInfo[] SYMBOLS = {
            new DataMatrixSymbolInfo(false, 3, 5, 8, 8, 1),
            new DataMatrixSymbolInfo(false, 5, 7, 10, 10, 1),
            /* rect */ new DataMatrixSymbolInfo(true, 5, 7, 16, 6, 1),
            new DataMatrixSymbolInfo(false, 8, 10, 12, 12, 1),
            /* rect */ new DataMatrixSymbolInfo(true, 10, 11, 14, 6, 2),
            new DataMatrixSymbolInfo(false, 12, 12, 14, 14, 1),
            /* rect */ new DataMatrixSymbolInfo(true, 16, 14, 24, 10, 1),

            new DataMatrixSymbolInfo(false, 18, 14, 16, 16, 1),
            new DataMatrixSymbolInfo(false, 22, 18, 18, 18, 1),
            /* rect */ new DataMatrixSymbolInfo(true, 22, 18, 16, 10, 2),
            new DataMatrixSymbolInfo(false, 30, 20, 20, 20, 1),
            /* rect */ new DataMatrixSymbolInfo(true, 32, 24, 16, 14, 2),
            new DataMatrixSymbolInfo(false, 36, 24, 22, 22, 1),
            new DataMatrixSymbolInfo(false, 44, 28, 24, 24, 1),
            /* rect */ new DataMatrixSymbolInfo(true, 49, 28, 22, 14, 2),

            new DataMatrixSymbolInfo(false, 62, 36, 14, 14, 4),
            new DataMatrixSymbolInfo(false, 86, 42, 16, 16, 4),
            new DataMatrixSymbolInfo(false, 114, 48, 18, 18, 4),
            new DataMatrixSymbolInfo(false, 144, 56, 20, 20, 4),
            new DataMatrixSymbolInfo(false, 174, 68, 22, 22, 4),

            new DataMatrixSymbolInfo(false, 204, 84, 24, 24, 4, 102, 42),
            new DataMatrixSymbolInfo(false, 280, 112, 14, 14, 16, 140, 56),
            new DataMatrixSymbolInfo(false, 368, 144, 16, 16, 16, 92, 36),
            new DataMatrixSymbolInfo(false, 456, 192, 18, 18, 16, 114, 48),
            new DataMatrixSymbolInfo(false, 576, 224, 20, 20, 16, 144, 56),
            new DataMatrixSymbolInfo(false, 696, 272, 22, 22, 16, 174, 68),
            new DataMatrixSymbolInfo(false, 816, 336, 24, 24, 16, 136, 56),
            new DataMatrixSymbolInfo(false, 1050, 408, 18, 18, 36, 175, 68),
            new DataMatrixSymbolInfo(false, 1304, 496, 20, 20, 36, 163, 62),
            new DataMatrixSymbolInfo144()
    };

    private final boolean rectangular;
    private final int dataCapacity;
    private final int errorCodewords;
    private final int matrixWidth;
    private final int matrixHeight;
    private final int dataRegions;
    private final int rsBlockData;
    private final int rsBlockError;

    DataMatrixSymbolInfo(boolean rectangular, int dataCapacity, int errorCodewords,
                         int matrixWidth, int matrixHeight, int dataRegions) {
        this(rectangular, dataCapacity, errorCodewords, matrixWidth, matrixHeight, dataRegions,
                dataCapacity, errorCodewords);
    }

    DataMatrixSymbolInfo(boolean rectangular, int dataCapacity, int errorCodewords,
                         int matrixWidth, int matrixHeight, int dataRegions,
                         int rsBlockData, int rsBlockError) {
        this.rectangular = rectangular;
        this.dataCapacity = dataCapacity;
        this.errorCodewords = errorCodewords;
        this.matrixWidth = matrixWidth;
        this.matrixHeight = matrixHeight;
        this.dataRegions = dataRegions;
        this.rsBlockData = rsBlockData;
        this.rsBlockError = rsBlockError;
    }

    public static DataMatrixSymbolInfo lookup(int dataCodewords, Shape shape) {
        for (DataMatrixSymbolInfo symbol : SYMBOLS) {
            if (shape == Shape.SQUARE && symbol.rectangular) {
                continue;
            }
            if (shape == Shape.RECTANGLE && !symbol.rectangular) {
                continue;
            }
            if (dataCodewords <= symbol.dataCapacity) {
                return symbol;
            }
        }
        throw new IllegalArgumentException(
                "No Data Matrix symbol fits " + dataCodewords + " data codewords");
    }

    private int horizontalDataRegions() {
        return switch (dataRegions) {
            case 1 -> 1;
            case 2, 4 -> 2;
            case 16 -> 4;
            case 36 -> 6;
            default -> throw new IllegalStateException("Unsupported data region count: " + dataRegions);
        };
    }

    private int verticalDataRegions() {
        return switch (dataRegions) {
            case 1, 2 -> 1;
            case 4 -> 2;
            case 16 -> 4;
            case 36 -> 6;
            default -> throw new IllegalStateException("Unsupported data region count: " + dataRegions);
        };
    }

    public int symbolDataWidth() {
        return horizontalDataRegions() * matrixWidth;
    }

    public int symbolDataHeight() {
        return verticalDataRegions() * matrixHeight;
    }

    public int symbolWidth() {
        return symbolDataWidth() + horizontalDataRegions() * 2;
    }

    public int symbolHeight() {
        return symbolDataHeight() + verticalDataRegions() * 2;
    }

    public boolean rectangular() {
        return rectangular;
    }

    public int dataCapacity() {
        return dataCapacity;
    }

    public int errorCodewords() {
        return errorCodewords;
    }

    public int matrixWidth() {
        return matrixWidth;
    }

    public int matrixHeight() {
        return matrixHeight;
    }

    public int interleavedBlockCount() {
        return dataCapacity / rsBlockData;
    }

    public int dataLengthForInterleavedBlock(int index) {
        return rsBlockData;
    }

    public int errorLengthForInterleavedBlock(int index) {
        return rsBlockError;
    }
}
