package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.util.List;

import com.google.zxing.BinaryBitmap;
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

    @Test
    void decodedTextEqualsInput() throws Exception {
        List<String> texts = List.of(
                "Bars4J",
                "HELLO WORLD",
                "0123456789",
                "https://dataprev.gov.br",
                "QRCode gerado pela Bars4J");

        for (String text : texts) {
            BufferedImage image = BarcodeGenerator.of("QRCODE")
                    .with(new RenderSpec(1, 70, 2.0, 8, java.awt.Color.BLACK, java.awt.Color.WHITE))
                    .toImage(text);
            Result result = decode(image);
            assertEquals(text, result.getText(), "round-trip failed for '" + text + "'");
        }
    }

    private static Result decode(BufferedImage image) throws Exception {
        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        return new QRCodeReader().decode(bitmap);
    }
}
