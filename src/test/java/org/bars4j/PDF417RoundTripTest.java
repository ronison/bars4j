package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.pdf417.PDF417Reader;

import org.bars4j.encode.matrix.PDF417Encoder;
import org.bars4j.render.RenderSpec;
import org.bars4j.render.matrix.MatrixRenderer;
import org.junit.jupiter.api.Test;

/**
 * Renders the generated PDF417 symbols and decodes them back with ZXing, proving that the produced
 * matrices are valid, readable symbols.
 */
class PDF417RoundTripTest {

    private static final RenderSpec SPEC = new RenderSpec(1, 70, 2.0, 4, Color.BLACK, Color.WHITE);

    @Test
    void decodedTextEqualsInput() throws Exception {
        List<String> texts = List.of(
                "1",
                "1234567890",
                "Bars4J",
                "HELLO WORLD",
                "Hello, world!",
                "PDF417 gerado pela Bars4J",
                "https://dataprev.gov.br",
                "The quick brown fox jumps over the lazy dog.",
                "1".repeat(200),
                "PDF417 round trip ".repeat(10));
        PDF417Encoder encoder = new PDF417Encoder();
        for (String text : texts) {
            BufferedImage image = new MatrixRenderer().toImage(encoder.encode(text), SPEC);
            assertEquals(text, decode(image).getText(), "round-trip failed for '" + text + "'");
        }
    }

    private static Result decode(BufferedImage image) throws Exception {
        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        return new PDF417Reader().decode(bitmap);
    }
}
