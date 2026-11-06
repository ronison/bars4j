package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import org.bars4j.api.BarcodeGenerator;
import org.bars4j.encode.api.BarcodeException;
import org.bars4j.encode.api.UnsupportedTextException;
import org.bars4j.encode.matrix.AztecEncoder;
import org.bars4j.encode.matrix.DataMatrixEncoder;
import org.bars4j.encode.matrix.PDF417Encoder;
import org.bars4j.encode.matrix.QRCodeEncoder;
import org.bars4j.encode.matrix.datamatrix.DataMatrixSymbolInfo;
import org.bars4j.encode.matrix.qr.QrEccLevel;
import org.bars4j.encode.matrix.qr.QrVersion;
import org.bars4j.symbol.MatrixSymbol;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Boundary-condition and input-validation tests for the four matrix (2D) formats.
 *
 * <p>Covers the capacity limits (largest fitting symbol / one below), forced-size parameters, the
 * empty-input policy (QRCode and Aztec allow it, Data Matrix and PDF417 do not), charset limits
 * (Data Matrix is ISO-8859-1 only) and the guarantee that oversized input surfaces as a checked
 * {@link UnsupportedTextException} rather than a leaked {@link IllegalArgumentException}.
 */
class MatrixBoundaryTest {

    private static final List<String> MATRIX_IDS = List.of("QRCODE", "DATAMATRIX", "PDF417", "AZTEC");

    private static void assertCheckedOrSuccess(String id, String text) {
        try {
            assertNotNull(BarcodeGenerator.of(id).toImage(text));
        } catch (BarcodeException expected) {
            // Desired outcome for input the format cannot represent.
        } catch (RuntimeException leaked) {
            fail(id + " leaked an unchecked exception for input of length " + text.length()
                    + ": " + leaked);
        }
    }

    // -------------------------------------------------------------------------------------------
    // QRCode
    // -------------------------------------------------------------------------------------------

    @Test
    void qrAcceptsEmptyInput() throws Exception {
        MatrixSymbol symbol = new QRCodeEncoder().encode("");
        assertEquals(21, symbol.widthInModules()); // version 1
        assertNotNull(BarcodeGenerator.of("QRCODE").toImage(""));
    }

    @Test
    void qrRejectsInputTooLargeForVersion40() {
        QRCodeEncoder encoder = new QRCodeEncoder(QrEccLevel.M, StandardCharsets.ISO_8859_1, null);
        UnsupportedTextException ex = assertThrows(UnsupportedTextException.class,
                () -> encoder.encode("x".repeat(3000)));
        assertTrue(ex.getMessage().contains("too large"), ex.getMessage());
    }

    @Test
    void qrRejectsDataThatDoesNotFitAForcedVersion() {
        QRCodeEncoder encoder = new QRCodeEncoder(QrEccLevel.M, StandardCharsets.ISO_8859_1, 1);
        assertThrows(UnsupportedTextException.class, () -> encoder.encode("x".repeat(100)));
    }

    @Test
    void qrVersionNumberOutsideOneToFortyIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> QrVersion.getVersionForNumber(0));
        assertThrows(IllegalArgumentException.class, () -> QrVersion.getVersionForNumber(41));
    }

    @Test
    void qrReplacesCharactersOutsideTheConfiguredCharsetInsteadOfFailing() throws Exception {
        assertNotNull(BarcodeGenerator.of("QRCODE").toImage("caf\u00E9 \uD83D\uDE00"));
    }

    // -------------------------------------------------------------------------------------------
    // Data Matrix
    // -------------------------------------------------------------------------------------------

    @Test
    void dataMatrixRejectsEmptyInput() {
        UnsupportedTextException ex = assertThrows(UnsupportedTextException.class,
                () -> new DataMatrixEncoder().encode(""));
        assertTrue(ex.getMessage().contains("non-empty"), ex.getMessage());
    }

    @Test
    void dataMatrixRejectsCharactersOutsideLatin1() {
        UnsupportedTextException ex = assertThrows(UnsupportedTextException.class,
                () -> new DataMatrixEncoder().encode("a\u20ACb"));
        assertTrue(ex.getMessage().contains("ISO-8859-1"), ex.getMessage());
    }

    @Test
    void dataMatrixAcceptsHighLatin1Bytes() throws Exception {
        assertNotNull(new DataMatrixEncoder().encode("caf\u00E9 \u00FF"));
    }

    @Test
    void dataMatrixCapacityBoundary() throws Exception {
        // 1558 data codewords is exactly the largest ECC200 symbol (144x144).
        assertNotNull(new DataMatrixEncoder().encode("A".repeat(1558)));
        assertThrows(UnsupportedTextException.class, () -> new DataMatrixEncoder().encode("A".repeat(1559)));
    }

    @Test
    void dataMatrixShapeConstraintRespectsRectangularCapacity() throws Exception {
        DataMatrixEncoder rectangular = new DataMatrixEncoder(DataMatrixSymbolInfo.Shape.RECTANGLE);
        assertNotNull(rectangular.encode("A".repeat(49)));
        assertThrows(UnsupportedTextException.class, () -> rectangular.encode("A".repeat(50)));
    }

    // -------------------------------------------------------------------------------------------
    // PDF417
    // -------------------------------------------------------------------------------------------

    @Test
    void pdf417RejectsEmptyInput() {
        UnsupportedTextException ex = assertThrows(UnsupportedTextException.class,
                () -> new PDF417Encoder().encode(""));
        assertTrue(ex.getMessage().contains("Empty"), ex.getMessage());
    }

    @Test
    void pdf417RejectsNonEncodableCharacters() {
        assertThrows(UnsupportedTextException.class, () -> new PDF417Encoder().encode("a\u20ACb"));
    }

    @Test
    void pdf417RejectsInputBeyondItsCodewordBudget() {
        assertThrows(UnsupportedTextException.class,
                () -> new PDF417Encoder().encode("\u00FF".repeat(2000)));
    }

    @Test
    void pdf417RejectsErrorCorrectionLevelOutsideZeroToEight() {
        assertThrows(IllegalArgumentException.class, () -> new PDF417Encoder(-1));
        assertThrows(IllegalArgumentException.class, () -> new PDF417Encoder(9));
    }

    @Test
    void pdf417AcceptsEveryValidErrorCorrectionLevel() throws Exception {
        for (int level = 0; level <= 8; level++) {
            assertNotNull(new PDF417Encoder(level).encode("Bars4J"));
        }
    }

    // -------------------------------------------------------------------------------------------
    // Aztec
    // -------------------------------------------------------------------------------------------

    @Test
    void aztecAcceptsEmptyInput() throws Exception {
        assertNotNull(new AztecEncoder().encode(""));
        assertNotNull(BarcodeGenerator.of("AZTEC").toImage(""));
    }

    @Test
    void aztecRejectsInputTooLargeForThirtyTwoLayers() {
        UnsupportedTextException ex = assertThrows(UnsupportedTextException.class,
                () -> new AztecEncoder().encode("x".repeat(5000)));
        assertTrue(ex.getMessage().contains("too large"), ex.getMessage());
    }

    @Test
    void aztecRejectsForcedLayerCountBeyondTheMaximum() {
        assertThrows(IllegalArgumentException.class, () -> new AztecEncoder(33, 33, null).encode("A"));
    }

    @Test
    void aztecRejectsDataThatDoesNotFitTheForcedLayer() {
        assertThrows(UnsupportedTextException.class,
                () -> new AztecEncoder(33, -1, null).encode("A".repeat(200)));
    }

    @Test
    void aztecReplacesCharactersOutsideTheConfiguredCharsetInsteadOfFailing() throws Exception {
        assertNotNull(BarcodeGenerator.of("AZTEC").toImage("caf\u00E9 \uD83D\uDE00"));
    }

    // -------------------------------------------------------------------------------------------
    // global resilience guard: adversarial input must never leak a RuntimeException
    // -------------------------------------------------------------------------------------------

    @TestFactory
    Stream<DynamicTest> adversarialInputNeverLeaksUncheckedException() {
        List<String> adversarial = List.of(
                "", "\u0000", "\u20AC", "\uD83D\uDE00", "\u00FF".repeat(2000), "x".repeat(5000));
        return MATRIX_IDS.stream().flatMap(id -> adversarial.stream()
                .map(text -> dynamicTest(id + ":len" + text.length(),
                        () -> assertCheckedOrSuccess(id, text))));
    }
}
