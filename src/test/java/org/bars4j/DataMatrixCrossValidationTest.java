package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.DataMatrixWriter;
import com.google.zxing.datamatrix.encoder.HighLevelEncoder;
import com.google.zxing.datamatrix.encoder.SymbolShapeHint;

import org.bars4j.encode.matrix.datamatrix.DataMatrixErrorCorrection;
import org.bars4j.encode.matrix.datamatrix.DataMatrixPlacement;
import org.bars4j.encode.matrix.datamatrix.DataMatrixSymbolAssembler;
import org.bars4j.encode.matrix.datamatrix.DataMatrixSymbolInfo;
import org.junit.jupiter.api.Test;

/**
 * Cross-validates the Data Matrix low-level pipeline (error correction, interleaving, module
 * placement and symbol assembly) against ZXing.
 *
 * <p>To isolate the low level from the choice of encodation scheme, ZXing is used to produce the
 * data codewords, which are then fed through the Bars4J implementation; the resulting symbol must be
 * module-for-module identical to ZXing's own symbol.
 */
class DataMatrixCrossValidationTest {

    @Test
    void matricesMatchZXing() {
        Set<Integer> sizesSeen = new LinkedHashSet<>();
        for (String text : inputs()) {
            boolean[][] expected = zxingMatrix(text);
            boolean[][] actual = bars4jMatrixUsingZXingCodewords(text);
            sizesSeen.add(expected.length);
            assertMatrixEquals(expected, actual, text);
        }
        assertTrue(sizesSeen.size() >= 8,
                "expected the inputs to exercise several symbol sizes, saw " + sizesSeen);
    }

    private static List<String> inputs() {
        return List.of(
                "1",
                "1234567890",
                "HELLO",
                "HELLO WORLD",
                "Hello, world!",
                "Bars4J Data Matrix",
                "https://dataprev.gov.br",
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",
                "The quick brown fox jumps over the lazy dog 0123456789",
                "A".repeat(120),
                "1".repeat(300),
                "DATA MATRIX ECC200 ".repeat(30));
    }

    private static boolean[][] zxingMatrix(String text) {
        BitMatrix matrix = new DataMatrixWriter().encode(text, BarcodeFormat.DATA_MATRIX, 0, 0);
        boolean[][] result = new boolean[matrix.getHeight()][matrix.getWidth()];
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                result[y][x] = matrix.get(x, y);
            }
        }
        return result;
    }

    private static boolean[][] bars4jMatrixUsingZXingCodewords(String text) {
        String encoded = HighLevelEncoder.encodeHighLevel(
                text, SymbolShapeHint.FORCE_NONE, null, null, false);
        int[] data = new int[encoded.length()];
        for (int i = 0; i < encoded.length(); i++) {
            data[i] = encoded.charAt(i);
        }
        DataMatrixSymbolInfo info = DataMatrixSymbolInfo.lookup(data.length, DataMatrixSymbolInfo.Shape.NONE);
        int[] full = DataMatrixErrorCorrection.encodeECC200(data, info);
        DataMatrixPlacement placement = new DataMatrixPlacement(
                full, info.symbolDataWidth(), info.symbolDataHeight());
        placement.place();
        return DataMatrixSymbolAssembler.assemble(placement, info);
    }

    private static void assertMatrixEquals(boolean[][] expected, boolean[][] actual, String text) {
        assertEquals(expected.length, actual.length, "height mismatch for '" + text + "'");
        assertEquals(expected[0].length, actual[0].length, "width mismatch for '" + text + "'");
        for (int y = 0; y < expected.length; y++) {
            for (int x = 0; x < expected[0].length; x++) {
                if (expected[y][x] != actual[y][x]) {
                    throw new AssertionError("module mismatch at (" + x + "," + y
                            + ") for '" + text + "'");
                }
            }
        }
    }
}
