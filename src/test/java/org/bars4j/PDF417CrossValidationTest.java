package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.zxing.pdf417.encoder.Compaction;
import com.google.zxing.pdf417.encoder.PDF417;

import org.bars4j.encode.matrix.PDF417Encoder;
import org.bars4j.symbol.MatrixSymbol;
import org.junit.jupiter.api.Test;

/**
 * Cross-validates the self-contained PDF417 encoder against the ZXing reference implementation
 * (test-scope dependency only). The generated matrix must match module for module.
 *
 * <p>ZXing returns its rows in the reverse visual order (its row 0 is the bottom of the symbol), so
 * the comparison flips the row index.
 */
class PDF417CrossValidationTest {

    @Test
    void matricesMatchZXing() throws Exception {
        Set<String> sizesSeen = new LinkedHashSet<>();
        for (String text : inputs()) {
            byte[][] reference = zxingMatrix(text);
            MatrixSymbol symbol = new PDF417Encoder().encode(text);

            int rows = symbol.heightInModules();
            int cols = symbol.widthInModules();
            sizesSeen.add(rows + "x" + cols);

            assertEquals(reference.length, rows, "row count for '" + text + "'");
            assertEquals(reference[0].length, cols, "column count for '" + text + "'");

            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    boolean expected = reference[rows - 1 - row][col] != 0;
                    assertEquals(expected, symbol.module(row, col),
                            "module " + row + "," + col + " for '" + text + "'");
                }
            }
        }
        assertTrue(sizesSeen.size() >= 4,
                "expected the inputs to exercise several symbol sizes, saw " + sizesSeen);
    }

    private static List<String> inputs() {
        return List.of(
                "1",
                "12345678901234567890",
                "HELLO",
                "Bars4J",
                "Hello, world!",
                "https://dataprev.gov.br",
                "PDF417 cross validation",
                "The quick brown fox jumps over the lazy dog 0123456789",
                "A".repeat(60),
                "1".repeat(120),
                "ABCdef123!".repeat(20),
                "PDF417 ECC ".repeat(30));
    }

    private static byte[][] zxingMatrix(String text) throws Exception {
        PDF417 encoder = new PDF417(false);
        encoder.setCompaction(Compaction.AUTO);
        encoder.setEncoding(StandardCharsets.ISO_8859_1);
        encoder.generateBarcodeLogic(text, 2);
        return encoder.getBarcodeMatrix().getMatrix();
    }
}
