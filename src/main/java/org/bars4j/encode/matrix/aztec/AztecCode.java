package org.bars4j.encode.matrix.aztec;

/**
 * Metadata and module grid of an encoded Aztec symbol, as produced by {@link AztecSymbolEncoder}.
 */
public final class AztecCode {

    private final boolean compact;
    private final int layers;
    private final int codeWords;
    private final AztecBitMatrix matrix;

    AztecCode(boolean compact, int layers, int codeWords, AztecBitMatrix matrix) {
        this.compact = compact;
        this.layers = layers;
        this.codeWords = codeWords;
        this.matrix = matrix;
    }

    /** Returns {@code true} if this is a compact symbol (as opposed to a full symbol). */
    public boolean compact() {
        return compact;
    }

    /** Returns the number of layers. */
    public int layers() {
        return layers;
    }

    /** Returns the number of data codewords (not counting error correction words). */
    public int codeWords() {
        return codeWords;
    }

    /** Returns the symbol size (width and height) in modules. */
    public int size() {
        return matrix.size();
    }

    /** Returns the modules as {@code modules[row][column]}, row 0 at the top. */
    public boolean[][] modules() {
        return matrix.toModules();
    }
}
