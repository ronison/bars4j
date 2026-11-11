package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.bars4j.api.BarcodeGenerator;
import org.bars4j.encode.api.BarcodeException;
import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.encode.linear.CodabarEncoder;
import org.bars4j.encode.linear.Code11Encoder;
import org.bars4j.encode.linear.Code128Encoder;
import org.bars4j.encode.linear.Code39Encoder;
import org.bars4j.encode.linear.Code39ExtEncoder;
import org.bars4j.encode.linear.Code93Encoder;
import org.bars4j.encode.linear.Code93ExtEncoder;
import org.bars4j.encode.linear.Datalogic2of5Encoder;
import org.bars4j.encode.linear.Ean13Encoder;
import org.bars4j.encode.linear.Ean5Encoder;
import org.bars4j.encode.linear.Ean8Encoder;
import org.bars4j.encode.linear.Iata2of5Encoder;
import org.bars4j.encode.linear.Interleaved2of5Encoder;
import org.bars4j.encode.linear.Itf14Encoder;
import org.bars4j.encode.linear.Matrix2of5Encoder;
import org.bars4j.encode.linear.MsiPlesseyEncoder;
import org.bars4j.encode.linear.PostNetEncoder;
import org.bars4j.encode.linear.Standard2of5Encoder;
import org.bars4j.encode.linear.UpcaEncoder;
import org.bars4j.encode.linear.UpceEncoder;
import org.bars4j.format.BarcodeRegistry;
import org.bars4j.symbol.SymbolKind;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Boundary-condition and input-validation tests for the 19 linear (1D) formats.
 *
 * <p>Every 1D encoder must reject empty, too-short, too-long and invalid-character input with a
 * checked {@link InvalidSymbolException} (never a bare {@code RuntimeException}), both through the
 * public {@link BarcodeGenerator} API and at the raw encoder layer. Length boundaries are checked
 * per format, including the check-digit appending performed by the API facade (which shifts the
 * accepted user length by one for formats with an external check digit).
 */
class LinearBoundaryTest {

    // -------------------------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------------------------

    private static List<String> linearIds() {
        BarcodeRegistry registry = BarcodeRegistry.getInstance();
        return registry.ids().stream()
                .filter(id -> registry.get(id).spec().kind() == SymbolKind.LINEAR)
                .sorted()
                .toList();
    }

    private static Map<String, LinearEncoder> rawEncoders() {
        return Map.ofEntries(
                Map.entry("EAN13", new Ean13Encoder()),
                Map.entry("EAN8", new Ean8Encoder()),
                Map.entry("EAN5", new Ean5Encoder()),
                Map.entry("UPCA", new UpcaEncoder()),
                Map.entry("UPCE", new UpceEncoder()),
                Map.entry("CODABAR", new CodabarEncoder()),
                Map.entry("CODE39", new Code39Encoder()),
                Map.entry("CODE39EXT", new Code39ExtEncoder()),
                Map.entry("CODE93", new Code93Encoder()),
                Map.entry("CODE93EXT", new Code93ExtEncoder()),
                Map.entry("CODE128", new Code128Encoder()),
                Map.entry("CODE11", new Code11Encoder()),
                Map.entry("MSIPLESSEY", new MsiPlesseyEncoder()),
                Map.entry("STANDARD2OF5", new Standard2of5Encoder()),
                Map.entry("IATA2OF5", new Iata2of5Encoder()),
                Map.entry("MATRIX2OF5", new Matrix2of5Encoder()),
                Map.entry("DATALOGIC2OF5", new Datalogic2of5Encoder()),
                Map.entry("INTERLEAVED2OF5", new Interleaved2of5Encoder()),
                Map.entry("ITF14", new Itf14Encoder()),
                Map.entry("POSTNET", new PostNetEncoder()));
    }

    /** A text that each format must reject (wrong characters for its charset). */
    private static final Map<String, String> INVALID_TEXT = Map.ofEntries(
            Map.entry("EAN13", "789100031550X"),
            Map.entry("EAN8", "9788515X"),
            Map.entry("EAN5", "5449X"),
            Map.entry("UPCA", "0756781641X"),
            Map.entry("UPCE", "075678X"),
            Map.entry("CODABAR", "abc"),
            Map.entry("CODE39", "BARS4J!"),
            Map.entry("CODE39EXT", "\u00E9"),        // é: code >= 128
            Map.entry("CODE93", "BARS4J!"),
            Map.entry("CODE93EXT", "\u20AC"),        // €: not a Code93 special
            Map.entry("CODE128", "\u0001"),          // control byte: not in code B
            Map.entry("CODE11", "12A4"),
            Map.entry("MSIPLESSEY", "12A4"),
            Map.entry("STANDARD2OF5", "97885150A"),
            Map.entry("IATA2OF5", "97885150A"),
            Map.entry("MATRIX2OF5", "12345A"),
            Map.entry("DATALOGIC2OF5", "12345A"),
            Map.entry("INTERLEAVED2OF5", "12345A"),
            Map.entry("ITF14", "154001412887A"),
            Map.entry("POSTNET", "80536596A"));

    /** Formats whose checksum is a real value (Code128/Codabar return an empty checksum). */
    private static final List<String> IDS_WITH_REAL_CHECKSUM = List.of(
            "EAN13", "EAN8", "EAN5", "UPCA", "UPCE", "CODE39", "CODE39EXT", "CODE93", "CODE93EXT",
            "CODE11", "MSIPLESSEY", "STANDARD2OF5", "IATA2OF5", "MATRIX2OF5", "DATALOGIC2OF5",
            "INTERLEAVED2OF5", "ITF14", "POSTNET");

    private static void assertCheckedOrSuccess(String id, String text) {
        try {
            BufferedImage image = BarcodeGenerator.of(id).toImage(text);
            assertNotNull(image);
        } catch (BarcodeException expected) {
            // A checked encoding failure is the desired outcome for invalid input.
        } catch (RuntimeException leaked) {
            fail(id + " leaked an unchecked exception for input \"" + text + "\": " + leaked);
        }
    }

    // -------------------------------------------------------------------------------------------
    // empty input
    // -------------------------------------------------------------------------------------------

    @TestFactory
    Stream<DynamicTest> publicApiRejectsEmptyInput() {
        return linearIds().stream().map(id -> dynamicTest(id, () ->
                assertThrows(InvalidSymbolException.class, () -> BarcodeGenerator.of(id).toImage(""))));
    }

    @TestFactory
    Stream<DynamicTest> rawEncoderRejectsEmptyInput() {
        return rawEncoders().entrySet().stream().map(entry -> dynamicTest(entry.getKey(), () ->
                assertThrows(InvalidSymbolException.class, () -> entry.getValue().encode(""))));
    }

    // -------------------------------------------------------------------------------------------
    // invalid characters
    // -------------------------------------------------------------------------------------------

    @TestFactory
    Stream<DynamicTest> publicApiRejectsInvalidCharacters() {
        return INVALID_TEXT.entrySet().stream().map(entry -> dynamicTest(entry.getKey(), () -> {
            InvalidSymbolException ex = assertThrows(InvalidSymbolException.class,
                    () -> BarcodeGenerator.of(entry.getKey()).toImage(entry.getValue()));
            assertNotNull(ex.getMessage());
        }));
    }

    @Test
    void invalidCharacterFixturesCoverEveryLinearFormat() {
        assertEquals(new HashSet<>(linearIds()), new HashSet<>(INVALID_TEXT.keySet()));
    }

    @TestFactory
    Stream<DynamicTest> checkSumRejectsInvalidCharacters() {
        return IDS_WITH_REAL_CHECKSUM.stream().map(id -> dynamicTest(id, () ->
                assertThrows(InvalidSymbolException.class,
                        () -> BarcodeGenerator.of(id).checkSum(INVALID_TEXT.get(id)))));
    }

    // -------------------------------------------------------------------------------------------
    // raw length boundaries (fixed-length symbologies)
    // -------------------------------------------------------------------------------------------

    @Test
    void ean13RawLengthBoundaries() throws Exception {
        Ean13Encoder encoder = new Ean13Encoder();
        encoder.encode("789100031550");    // 12
        encoder.encode("7891000315507");   // 13 (valid check digit)
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("78910003155"));    // 11
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("78910003155000")); // 14
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("7891000315500"));  // 13, wrong check digit
    }

    @Test
    void ean8RawLengthBoundaries() throws Exception {
        Ean8Encoder encoder = new Ean8Encoder();
        encoder.encode("97885153");   // 8 (valid check digit)
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("9788515"));   // 7
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("978851500")); // 9
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("97885150"));  // 8, wrong check digit
    }

    @Test
    void ean5RawLengthBoundaries() throws Exception {
        Ean5Encoder encoder = new Ean5Encoder();
        encoder.encode("54495");   // 5
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("5449"));   // 4
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("544955")); // 6
    }

    @Test
    void upcaRawLengthBoundaries() throws Exception {
        UpcaEncoder encoder = new UpcaEncoder();
        encoder.encode("07567816415");    // 11
        encoder.encode("075678164156");   // 12 (valid check digit)
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("0756781641"));    // 10
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("0756781641500")); // 13
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("075678164151"));  // 12, wrong check digit
    }

    @Test
    void upceRawLengthBoundaries() throws Exception {
        UpceEncoder encoder = new UpceEncoder();
        String data = "0123456";
        String full = data + encoder.computeCheckSum(data);
        assertEquals(8, full.length());
        encoder.encode(full);   // 8 (7 data + check digit)
        assertThrows(InvalidSymbolException.class, () -> encoder.encode(data));       // 7
        assertThrows(InvalidSymbolException.class, () -> encoder.encode(full + "0")); // 9
        assertThrows(InvalidSymbolException.class, () -> encoder.encode(data + "0")); // wrong check digit
    }

    // -------------------------------------------------------------------------------------------
    // check-digit aware boundaries through the public API
    // -------------------------------------------------------------------------------------------

    @Test
    void ean13ApiShiftsAcceptedLengthByTheAppendedCheckDigit() throws Exception {
        BarcodeGenerator generator = BarcodeGenerator.of("EAN13");
        generator.toImage("78910003155");    // 11 user digits -> 12 encoded
        generator.toImage("789100031550");   // 12 user digits -> 13 encoded
        generator.toImage("7891000315507");  // 13 digits: complete code, valid check digit
        assertThrows(BarcodeException.class, () -> generator.toImage("7891000315"));    // 10 -> 11
        assertThrows(BarcodeException.class, () -> generator.toImage("7891000315500")); // 13 digits, wrong check digit
    }

    @Test
    void ean8ApiShiftsAcceptedLengthByTheAppendedCheckDigit() throws Exception {
        BarcodeGenerator generator = BarcodeGenerator.of("EAN8");
        generator.toImage("9788515");   // 7 -> 8
        generator.toImage("97885153");  // 8 digits: complete code, valid check digit
        assertThrows(BarcodeException.class, () -> generator.toImage("978851"));   // 6 -> 7
        assertThrows(BarcodeException.class, () -> generator.toImage("97885150")); // 8 digits, wrong check digit
    }

    @Test
    void upceApiRequiresExactlySevenDigitsBeforeTheCheckDigit() throws Exception {
        BarcodeGenerator generator = BarcodeGenerator.of("UPCE");
        generator.toImage("0756785");   // 7 -> 8
        generator.toImage("07567858");  // 8 digits: complete code, valid check digit
        assertThrows(BarcodeException.class, () -> generator.toImage("075678"));    // 6
        assertThrows(BarcodeException.class, () -> generator.toImage("07567850")); // 8 digits, wrong check digit
    }

    @Test
    void code11CheckSumUsesItsFullWidth() throws Exception {
        // The C symbol is a mod-11 value, so it can be a single digit or "10"; no fixed width.
        Code11Encoder encoder = new Code11Encoder();
        assertTrue(encoder.computeCheckSum("123456").length() >= 1);
        assertTrue(encoder.computeCheckSum("1234567890").length() >= 2);
    }

    // -------------------------------------------------------------------------------------------
    // format-specific quirks
    // -------------------------------------------------------------------------------------------

    @Test
    void interleaved2of5RejectsOddLengthAndNonDigit() throws Exception {
        Interleaved2of5Encoder encoder = new Interleaved2of5Encoder();
        // Odd digit lengths are rejected: the pattern encodes digit pairs and the old behaviour
        // silently dropped the trailing digit.
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("123"));
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("12A"));
        assertNotNull(encoder.encode("1234"));
    }

    @Test
    void itf14RawLengthAndCheckDigitBoundaries() throws Exception {
        Itf14Encoder encoder = new Itf14Encoder();
        encoder.encode("1540014128876");    // 13 data digits (check digit computed)
        encoder.encode("15400141288763");   // 14 digits (valid check digit)
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("154001412887"));    // 12
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("154001412887630")); // 15
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("15400141288764"));  // wrong check digit
    }

    @Test
    void itf14ApiAcceptsPayloadOrCompleteCode() throws Exception {
        BarcodeGenerator generator = BarcodeGenerator.of("ITF14");
        generator.toImage("1540014128876");   // 13 data digits -> check digit appended
        generator.toImage("15400141288763");  // 14 digits: complete code, valid check digit
        assertThrows(BarcodeException.class, () -> generator.toImage("154001412887"));   // 12
        assertThrows(BarcodeException.class, () -> generator.toImage("15400141288764")); // wrong check digit
    }

    @Test
    void code39ExtendedRejectsCodeAboveAscii() {
        Code39ExtEncoder encoder = new Code39ExtEncoder();
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("\u00E9"));
    }

    @Test
    void code93ExtendedRejectsCharactersOutsideItsCharset() {
        Code93ExtEncoder encoder = new Code93ExtEncoder();
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("\u20AC"));
    }

    @Test
    void msiPlesseyRejectsEmptyAndNonDigits() throws Exception {
        MsiPlesseyEncoder encoder = new MsiPlesseyEncoder();
        assertThrows(InvalidSymbolException.class, () -> encoder.encode(""));
        assertThrows(InvalidSymbolException.class, () -> encoder.computeCheckSum(""));
        assertThrows(InvalidSymbolException.class, () -> encoder.computeCheckSum("12A"));
        encoder.encode("123456");
    }

    // -------------------------------------------------------------------------------------------
    // large, but valid, input must still work
    // -------------------------------------------------------------------------------------------

    @Test
    void variableLengthFormatsAcceptLongInput() throws Exception {
        assertNotNull(BarcodeGenerator.of("CODE128").toImage("A".repeat(400)));
        assertNotNull(BarcodeGenerator.of("CODE39").toImage("A".repeat(400)));
        assertNotNull(BarcodeGenerator.of("CODE93").toImage("A".repeat(400)));
        assertNotNull(BarcodeGenerator.of("CODABAR").toImage("1".repeat(400)));
        assertNotNull(BarcodeGenerator.of("INTERLEAVED2OF5").toImage("1".repeat(399)));
    }

    // -------------------------------------------------------------------------------------------
    // global resilience guard: adversarial input must never leak a RuntimeException
    // -------------------------------------------------------------------------------------------

    @TestFactory
    Stream<DynamicTest> adversarialInputNeverLeaksUncheckedException() {
        List<String> adversarial = List.of(
                "", "\u0000", "\u0001", " ", "x", "12A", "!", "\u00E9", "\u20AC",
                "\uD83D\uDE00", "9".repeat(500));
        return linearIds().stream().flatMap(id -> adversarial.stream()
                .map(text -> dynamicTest(id + ":" + describe(text),
                        () -> assertCheckedOrSuccess(id, text))));
    }

    private static String describe(String text) {
        if (text.length() > 8) {
            return "len" + text.length();
        }
        return text.isEmpty() ? "empty" : text;
    }
}
