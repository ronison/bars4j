package org.bars4j.encode.matrix.datamatrix;

/**
 * The 144x144 ECC200 symbol, whose ten Reed-Solomon blocks are not all the same size.
 *
 * <p>The catalogue assumes uniform blocks, so this symbol overrides the block layout: the first
 * eight blocks carry 156 data codewords and the last two carry 155. Error codewords stay uniform.
 */
final class DataMatrixSymbolInfo144 extends DataMatrixSymbolInfo {

    /** Number of data codewords in each of the first eight (larger) blocks. */
    private static final int LARGE_BLOCK_DATA = 156;

    /** Number of data codewords in each of the two trailing (smaller) blocks. */
    private static final int SMALL_BLOCK_DATA = 155;

    /** Index up to which blocks are the larger size. */
    private static final int LARGE_BLOCK_COUNT = 8;

    DataMatrixSymbolInfo144() {
        super(false, 1558, 620, 22, 22, 6, 6, LARGE_BLOCK_DATA, 62);
    }

    @Override
    public int interleavedBlockCount() {
        return 10;
    }

    @Override
    public int dataLengthForInterleavedBlock(int index) {
        return index <= LARGE_BLOCK_COUNT ? LARGE_BLOCK_DATA : SMALL_BLOCK_DATA;
    }
}
