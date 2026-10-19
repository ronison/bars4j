package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import org.bars4j.api.BarcodeGenerator;
import org.bars4j.render.RenderSpec;
import org.junit.jupiter.api.Test;

/**
 * Phase 4: decode the generated QRCode image back with ZXing to prove it is a valid QRCode.
 */
class QRCodeRoundTripTest {

    private static final RenderSpec SPEC = new RenderSpec(1, 70, 2.0, 8, Color.BLACK, Color.WHITE);

    @Test
    void decodedTextEqualsInput() throws Exception {
        List<String> texts = List.of(
                "Bars4J",
                "HELLO WORLD",
                "0123456789",
                "https://dataprev.gov.br",
                "QRCode gerado pela Bars4J");
        for (String text : texts) {
            BufferedImage image = BarcodeGenerator.of("QRCODE").with(SPEC).toImage(text);
            assertEquals(text, decode(image, Map.of()).getText(), "round-trip failed for '" + text + "'");
        }
    }

    @Test
    void kanjiModeRoundTrip() throws Exception {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.CHARACTER_SET, "Shift_JIS");

        List<String> texts = List.of("漢字テスト", "日本語", "こんにちは世界");
        for (String text : texts) {
            BufferedImage image = BarcodeGenerator.of("QRCODE").with(SPEC).toImage(text);
            assertEquals(text, decode(image, hints).getText(), "kanji round-trip failed for '" + text + "'");
        }
    }

    @Test
    void highVersionRoundTrip() throws Exception {
        String text = "Bars4J ".repeat(200);
        BufferedImage image = BarcodeGenerator.of("QRCODE").with(SPEC).toImage(text);
        assertEquals(text, decode(image, Map.of()).getText());
    }

    private static Result decode(BufferedImage image, Map<DecodeHintType, Object> hints) throws Exception {
        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        return new QRCodeReader().decode(bitmap, hints);
    }
}
