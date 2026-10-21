package org.bars4j.encode.matrix.aztec;

/**
 * A square grid of Aztec modules addressed by {@code (x, y)} (column, row). Internally the modules
 * are kept row-major so that they can be handed to {@link org.bars4j.symbol.MatrixSymbol} directly.
 */
public final class AztecBitMatrix {

    private final int size;
    private final boolean[] bits;

    public AztecBitMatrix(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        this.size = size;
        this.bits = new boolean[size * size];
    }

    /** Returns the width/height of the symbol in modules. */
    public int size() {
        return size;
    }

    /** Marks the module at column {@code x}, row {@code y} as dark. */
    public void set(int x, int y) {
        bits[y * size + x] = true;
    }

    /** Returns {@code true} if the module at column {@code x}, row {@code y} is dark. */
    public boolean get(int x, int y) {
        return bits[y * size + x];
    }

    /** Returns the modules as {@code modules[row][column]}, row 0 at the top. */
    public boolean[][] toModules() {
        boolean[][] modules = new boolean[size][size];
        for (int y = 0; y < size; y++) {
            System.arraycopy(bits, y * size, modules[y], 0, size);
        }
        return modules;
    }
}
