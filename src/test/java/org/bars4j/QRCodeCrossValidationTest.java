package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.bars4j.encode.matrix.QRCodeEncoder;
import org.bars4j.encode.matrix.qr.QrEccLevel;
import org.bars4j.symbol.MatrixSymbol;
import org.junit.jupiter.api.Test;

/**
 * Phase 4 acceptance test: the self-contained QRCode encoder must produce matrices identical to
 * the ZXing reference implementation (test-scope dependency only).
 */
class QRCodeCrossValidationTest {

    private static BitMatrix zxing(String text, QrEccLevel level) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 0);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.valueOf(level.name()));
        return new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints);
    }

    private static List<String> inputs() {
        List<String> inputs = new ArrayList<>();
        inputs.add("1");
        inputs.add("0123456789");
        inputs.add("HELLO WORLD");
        inputs.add("Hello, world!");
        inputs.add("Bars4J-128");
        inputs.add("Bars4J QRCode cross validation");
        inputs.add(rep('A', 40));
        inputs.add(rep('a', 115));
        inputs.add(rep('7', 280));
        inputs.add(rep('Z', 170));
        inputs.add(rep('x', 118));
        inputs.add(rep('9', 260));
        return inputs;
    }

    private static String rep(char c, int count) {
        return String.valueOf(c).repeat(count);
    }

    @Test
    void matricesMatchZxing() throws Exception {
        Set<Integer> versionsSeen = new HashSet<>();
        for (String input : inputs()) {
            for (QrEccLevel level : QrEccLevel.values()) {
                MatrixSymbol symbol = new QRCodeEncoder(level).encode(input);
                BitMatrix reference = zxing(input, level);

                int size = symbol.widthInModules();
                versionsSeen.add((size - 17) / 4);
                assertEquals(reference.getWidth(), size, "size for '" + input + "' level " + level);

                for (int row = 0; row < size; row++) {
                    for (int col = 0; col < size; col++) {
                        assertEquals(reference.get(col, row), symbol.module(row, col),
                                "module " + row + "," + col + " for '" + input + "' level " + level);
                    }
                }
            }
        }
        assertTrue(versionsSeen.size() >= 6,
                "expected the inputs to exercise several versions, saw " + versionsSeen);
        assertTrue(versionsSeen.contains(1) && versionsSeen.contains(10),
                "expected versions 1 and 10 to be exercised, saw " + versionsSeen);
    }
}
