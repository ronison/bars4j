package org.bars4j.encode.matrix;

import org.bars4j.encode.api.MatrixEncoder;
import org.bars4j.encode.api.UnsupportedTextException;
import org.bars4j.encode.matrix.datamatrix.DataMatrixErrorCorrection;
import org.bars4j.encode.matrix.datamatrix.DataMatrixHighLevelEncoder;
import org.bars4j.encode.matrix.datamatrix.DataMatrixPlacement;
import org.bars4j.encode.matrix.datamatrix.DataMatrixSymbolAssembler;
import org.bars4j.encode.matrix.datamatrix.DataMatrixSymbolInfo;
import org.bars4j.symbol.MatrixSymbol;

/**
 * Self-contained Data Matrix (ECC200) encoder.
 *
 * <p>Implements the ISO/IEC 16022 square and rectangular symbols. The pipeline is: ASCII
 * encodation of the ISO-8859-1 input, filler padding up to the selected symbol capacity,
 * Reed-Solomon error protection with interleaving, module placement and finally finder-pattern
 * assembly. No external library is needed at run time.
 */
public final class DataMatrixEncoder implements MatrixEncoder {

    /** Quiet zone required by the Data Matrix standard, in modules. */
    private static final int QUIET_ZONE = 1;

    private final DataMatrixSymbolInfo.Shape shape;

    /** Creates an encoder that is free to pick any symbol shape. */
    public DataMatrixEncoder() {
        this(DataMatrixSymbolInfo.Shape.NONE);
    }

    /**
     * Creates an encoder restricted to the given shape.
     *
     * @param shape the desired symbol shape
     */
    public DataMatrixEncoder(DataMatrixSymbolInfo.Shape shape) {
        this.shape = shape;
    }

    @Override
    public MatrixSymbol encode(String text) throws UnsupportedTextException {
        int[] message = DataMatrixHighLevelEncoder.encodeToCodewords(text);
        DataMatrixSymbolInfo symbolInfo = DataMatrixSymbolInfo.lookup(message.length, shape);
        int[] codewords = protect(message, symbolInfo);
        boolean[][] modules = placeAndFrame(codewords, symbolInfo);
        return new MatrixSymbol(modules, QUIET_ZONE);
    }

    /** Pads the message to capacity and appends the Reed-Solomon error codewords. */
    private static int[] protect(int[] message, DataMatrixSymbolInfo symbolInfo) {
        int[] padded = DataMatrixHighLevelEncoder.padToCapacity(message, symbolInfo.dataCapacity());
        return DataMatrixErrorCorrection.encodeECC200(padded, symbolInfo);
    }

    /** Places the codewords over the data regions and wraps each region in its finder pattern. */
    private static boolean[][] placeAndFrame(int[] codewords, DataMatrixSymbolInfo symbolInfo) {
        DataMatrixPlacement placement = new DataMatrixPlacement(
                codewords, symbolInfo.symbolDataWidth(), symbolInfo.symbolDataHeight());
        placement.place();
        return DataMatrixSymbolAssembler.assemble(placement, symbolInfo);
    }

    @Override
    public String computeCheckSum(String text) {
        // Error correction is internal to the symbol; there is no external check digit.
        return "";
    }
}
