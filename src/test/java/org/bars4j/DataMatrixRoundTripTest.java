package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.datamatrix.DataMatrixReader;

import org.bars4j.api.BarcodeGenerator;
import org.bars4j.render.RenderSpec;
import org.junit.jupiter.api.Test;

/**
 * Decodes the generated Data Matrix images back with ZXing to prove they are valid symbols.
 */
class DataMatrixRoundTripTest {

    private static final RenderSpec SPEC = new RenderSpec(1, 70, 2.0, 8, Color.BLACK, Color.WHITE);

    @Test
    void decodedTextEqualsInput() throws Exception {
        List<String> texts = List.of(
                "Bars4J",
                "1234567890",
                "HELLO WORLD",
                "Hello, world!",
                "https://dataprev.gov.br",
                "Data Matrix ECC200 gerado pela Bars4J",
                "1".repeat(120),
                "ABC".repeat(100));
        for (String text : texts) {
            BufferedImage image = BarcodeGenerator.of("DATAMATRIX").with(SPEC).toImage(text);
            assertEquals(text, decode(image).getText(), "round-trip failed for '" + text + "'");
        }
    }

    private static Result decode(BufferedImage image) throws Exception {
        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        return new DataMatrixReader().decode(bitmap, Map.<DecodeHintType, Object>of());
    }
}
