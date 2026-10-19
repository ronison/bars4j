package org.bars4j.encode.matrix;

import org.bars4j.encode.api.MatrixEncoder;
import org.bars4j.encode.api.UnsupportedTextException;
import org.bars4j.encode.matrix.datamatrix.DataMatrixErrorCorrection;
import org.bars4j.encode.matrix.datamatrix.DataMatrixHighLevelEncoder;
import org.bars4j.encode.matrix.datamatrix.DataMatrixPlacement;
import org.bars4j.encode.matrix.datamatrix.DataMatrixSymbolInfo;
import org.bars4j.encode.matrix.datamatrix.DataMatrixSymbolAssembler;
import org.bars4j.symbol.MatrixSymbol;

/**
 * Self-contained Data Matrix (ECC200) encoder.
 *
 * <p>Supports the ISO/IEC 16022 square and rectangular symbols using ASCII encodation (ISO-8859-1
 * input) with full Reed-Solomon error correction, interleaving and the standard placement
 * algorithm. No runtime dependency on any external library.
 */
public final class DataMatrixEncoder implements MatrixEncoder {

    /** Quiet zone required by the Data Matrix standard, in modules. */
    private static final int QUIET_ZONE = 1;

    private final DataMatrixSymbolInfo.Shape shape;

    public DataMatrixEncoder() {
        this(DataMatrixSymbolInfo.Shape.NONE);
    }

    public DataMatrixEncoder(DataMatrixSymbolInfo.Shape shape) {
        this.shape = shape;
    }

    @Override
    public MatrixSymbol encode(String text) throws UnsupportedTextException {
        int[] codewords = DataMatrixHighLevelEncoder.encodeToCodewords(text);
        DataMatrixSymbolInfo symbolInfo = DataMatrixSymbolInfo.lookup(codewords.length, shape);
        int[] padded = DataMatrixHighLevelEncoder.padToCapacity(codewords, symbolInfo.dataCapacity());
        int[] full = DataMatrixErrorCorrection.encodeECC200(padded, symbolInfo);

        DataMatrixPlacement placement = new DataMatrixPlacement(
                full, symbolInfo.symbolDataWidth(), symbolInfo.symbolDataHeight());
        placement.place();

        boolean[][] modules = DataMatrixSymbolAssembler.assemble(placement, symbolInfo);
        return new MatrixSymbol(modules, QUIET_ZONE);
    }

    @Override
    public String computeCheckSum(String text) {
        // Data Matrix error correction is internal; there is no external check digit.
        return "";
    }
}
