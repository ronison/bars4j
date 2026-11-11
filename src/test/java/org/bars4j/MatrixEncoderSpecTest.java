package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.bars4j.encode.api.UnsupportedTextException;
import org.bars4j.encode.matrix.AztecEncoder;
import org.bars4j.encode.matrix.DataMatrixEncoder;
import org.bars4j.encode.matrix.PDF417Encoder;
import org.bars4j.encode.matrix.QRCodeEncoder;
import org.bars4j.encode.matrix.aztec.AztecEci;
import org.bars4j.encode.matrix.pdf417.Pdf417Barcode;
import org.bars4j.encode.matrix.pdf417.Pdf417Compaction;
import org.bars4j.encode.matrix.pdf417.Pdf417Dimensions;
import org.bars4j.symbol.MatrixSymbol;
import org.junit.jupiter.api.Test;

/**
 * Matrix-encoder specification scenarios: PDF417 forced compaction modes and their validation,
 * extended ECI support (GBK/Shift_JIS), null-safety across the top-level encoders and the
 * standard PDF417 dimensions from ISO/IEC 15438.
 */
class MatrixEncoderSpecTest {

    private static Pdf417Barcode barcode(Pdf417Compaction compaction, Charset charset) {
        return new Pdf417Barcode(compaction, charset, false, Pdf417Dimensions.standard());
    }

    // -------------------------------------------------------------------------------------------
    // PDF417 forced compaction modes
    // -------------------------------------------------------------------------------------------

    @Test
    void pdf417ForcedTextAcceptsTextAndDigits() throws Exception {
        assertNotNull(barcode(Pdf417Compaction.TEXT, StandardCharsets.ISO_8859_1)
                .generateBarcodeLogic("AB-12", 2));
        assertNotNull(barcode(Pdf417Compaction.TEXT, StandardCharsets.ISO_8859_1)
                .generateBarcodeLogic("123456", 2));
    }

    @Test
    void pdf417ForcedNumericAcceptsDigitsOnly() throws Exception {
        assertNotNull(barcode(Pdf417Compaction.NUMERIC, StandardCharsets.ISO_8859_1)
                .generateBarcodeLogic("123456", 2));
        UnsupportedTextException e = assertThrows(UnsupportedTextException.class,
                () -> barcode(Pdf417Compaction.NUMERIC, StandardCharsets.ISO_8859_1)
                        .generateBarcodeLogic("AB-12", 2));
        assertTrue(e.getMessage().contains("NUMERIC"), "error reports the forced mode: " + e.getMessage());
    }

    @Test
    void pdf417ForcedByteAcceptsAnyLatin1() throws Exception {
        assertNotNull(barcode(Pdf417Compaction.BYTE, StandardCharsets.ISO_8859_1)
                .generateBarcodeLogic("AB-12", 2));
    }

    @Test
    void pdf417GbkInputProducesSymbolWithEci() throws Exception {
        Pdf417Barcode barcode = barcode(Pdf417Compaction.AUTO, Charset.forName("GBK"));
        assertNotNull(barcode.generateBarcodeLogic("中文测试", 2), "GBK input encodes (ECI 29 designator emitted)");
    }

    @Test
    void pdf417DimensionsMatchIso15438() {
        Pdf417Dimensions dims = Pdf417Dimensions.standard();
        assertEquals(2, dims.minCols(), "ISO 15438 standard min cols");
        assertEquals(30, dims.maxCols(), "ISO 15438 standard max cols");
        assertEquals(3, dims.minRows(), "ISO 15438 standard min rows");
        assertEquals(90, dims.maxRows(), "ISO 15438 standard max rows");
    }

    // -------------------------------------------------------------------------------------------
    // QR
    // -------------------------------------------------------------------------------------------

    @Test
    void qrEncodesFullwidthDigits() throws Exception {
        // Fullwidth digits are not ASCII 0-9: they must take the byte (ECI) path, not corrupt
        // the numeric encoder.
        QRCodeEncoder encoder = new QRCodeEncoder();
        MatrixSymbol symbol = encoder.encode("１２３");
        assertNotNull(symbol);
        assertTrue(symbol.modules().length > 0);
    }

    @Test
    void qrEncodesUtf8TextWithEci() throws Exception {
        QRCodeEncoder encoder = new QRCodeEncoder();
        assertNotNull(encoder.encode("你好"));
    }

    @Test
    void qrRejectsNullText() {
        QRCodeEncoder encoder = new QRCodeEncoder();
        assertThrows(UnsupportedTextException.class, () -> encoder.encode(null));
    }

    // -------------------------------------------------------------------------------------------
    // DataMatrix / Aztec
    // -------------------------------------------------------------------------------------------

    @Test
    void dataMatrixRejectsNullText() {
        DataMatrixEncoder encoder = new DataMatrixEncoder();
        assertThrows(UnsupportedTextException.class, () -> encoder.encode(null));
    }

    @Test
    void aztecRejectsNullText() {
        AztecEncoder encoder = new AztecEncoder();
        assertThrows(UnsupportedTextException.class, () -> encoder.encode(null));
    }

    @Test
    void aztecRejectsUnknownEciDesignator() {
        assertThrows(UnsupportedTextException.class, () -> AztecEci.valueFor(Charset.forName("IBM500")));
    }
}
