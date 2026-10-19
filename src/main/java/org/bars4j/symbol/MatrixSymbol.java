package org.bars4j.symbol;

import java.util.Arrays;

/**
 * A matrix (2D) barcode symbol: a grid of modules.
 *
 * @param modules   {@code modules[row][column] == true} means a dark module
 * @param quietZone size of the mandatory quiet zone around the symbol, in modules
 */
public final class MatrixSymbol implements BarcodeSymbol {

    private final boolean[][] modules;
    private final int quietZone;

    public MatrixSymbol(boolean[][] modules, int quietZone) {
        if (modules.length == 0) {
            throw new IllegalArgumentException("modules must not be empty");
        }
        int width = modules[0].length;
        for (boolean[] row : modules) {
            if (row.length != width) {
                throw new IllegalArgumentException("modules must be rectangular");
            }
        }
        this.modules = deepCopy(modules);
        this.quietZone = quietZone;
    }

    private static boolean[][] deepCopy(boolean[][] source) {
        boolean[][] copy = new boolean[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = Arrays.copyOf(source[i], source[i].length);
        }
        return copy;
    }

    /** Returns a defensive copy of the module matrix. */
    public boolean[][] modules() {
        return deepCopy(modules);
    }

    public boolean module(int row, int column) {
        return modules[row][column];
    }

    public int quietZone() {
        return quietZone;
    }

    @Override
    public SymbolKind kind() {
        return SymbolKind.MATRIX;
    }

    @Override
    public int widthInModules() {
        return modules[0].length;
    }

    @Override
    public int heightInModules() {
        return modules.length;
    }
}
