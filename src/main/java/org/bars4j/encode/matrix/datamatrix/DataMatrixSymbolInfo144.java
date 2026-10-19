package org.bars4j.encode.matrix.datamatrix;

/** The special 144x144 Data Matrix symbol, which uses a non-uniform block layout. */
final class DataMatrixSymbolInfo144 extends DataMatrixSymbolInfo {

    DataMatrixSymbolInfo144() {
        super(false, 1558, 620, 22, 22, 36, -1, 62);
    }

    @Override
    public int interleavedBlockCount() {
        return 10;
    }

    @Override
    public int dataLengthForInterleavedBlock(int index) {
        return index <= 8 ? 156 : 155;
    }
}
