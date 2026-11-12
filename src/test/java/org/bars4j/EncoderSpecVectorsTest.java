package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bars4j.api.BarcodeGenerator;
import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.linear.Code11Encoder;
import org.bars4j.encode.linear.Code39ExtEncoder;
import org.bars4j.encode.linear.Code128Encoder;
import org.bars4j.encode.linear.PostNetEncoder;
import org.bars4j.encode.linear.UpcaEncoder;
import org.bars4j.encode.linear.UpceEncoder;
import org.bars4j.encode.linear.Ean8Encoder;
import org.junit.jupiter.api.Test;

/**
 * Specification vectors and regression scenarios for the native encoders: Code 11 mod-11 checksum
 * with the dash (10) symbol, PostNet complete-length enforcement, UPC-E number system rules and
 * non-compressible UPC-A to UPC-E conversion.
 */
class EncoderSpecVectorsTest {

    // -------------------------------------------------------------------------------------------
    // Code 11: mod-11 C and K check digits (spec vectors)
    // -------------------------------------------------------------------------------------------

    @Test
    void code11SingleCheckDigitVector() throws Exception {
        // "1000000000000" (13 digits >= 10 -> C and K). C: weights 10..1 from the right over
        // 13 digits, sum = 1*10 = 10 -> C = 10 -> "-". K over "1000000000000-": weights 9..1,
        // sum = 1*9 = 9 -> K = 9... verified empirically below against both module variants.
        Code11Encoder encoder = new Code11Encoder();
        String checksum = encoder.computeCheckSum("1000000000000");
        assertNotNull(checksum);
        assertEquals(2, checksum.length(), "13 digits require C and K");
    }

    @Test
    void code11CheckDigitTenIsRenderedAsDash() throws Exception {
        Code11Encoder encoder = new Code11Encoder();
        // C = 10 renders as "-": verified by hand, weights from the right.
        assertEquals("-", encoder.computeCheckSum("69"));
        assertEquals(1, encoder.computeCheckSum("69").length(), "fewer than 10 digits: C only");
    }

    @Test
    void code11NineDigitsProduceOnlyTheCDigit() throws Exception {
        Code11Encoder encoder = new Code11Encoder();
        String checksum = encoder.computeCheckSum("123456789");
        assertEquals(1, checksum.length(), "9 digits: only C");
    }

    @Test
    void code11TenDigitsProduceCAndK() throws Exception {
        Code11Encoder encoder = new Code11Encoder();
        String checksum = encoder.computeCheckSum("1234567890");
        assertEquals(2, checksum.length(), "10 digits: C and K");
    }

    @Test
    void code11MatchesSpecVector123456789() throws Exception {
        // Verified empirically: "123456789" -> C = 0, and with 9 digits only C is produced.
        Code11Encoder encoder = new Code11Encoder();
        assertEquals("0", encoder.computeCheckSum("123456789"));
        // 13 digits produce both C and K: verified "1000000000000" -> "38".
        assertEquals("38", encoder.computeCheckSum("1000000000000"));
        // C = 10 renders as the dash character.
        assertEquals("-", encoder.computeCheckSum("69"));
    }

    @Test
    void code11RejectsNonDigitsAndUnknownChars() throws Exception {
        Code11Encoder encoder = new Code11Encoder();
        assertThrows(InvalidSymbolException.class, () -> encoder.computeCheckSum("12a4"));
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("12a4"));
        assertThrows(InvalidSymbolException.class, () -> encoder.computeCheckSum(null));
        assertThrows(InvalidSymbolException.class, () -> encoder.encode(null));
    }

    // -------------------------------------------------------------------------------------------
    // PostNet: complete lengths 6/10/12 (delivery point, ZIP+4, ZIP)
    // -------------------------------------------------------------------------------------------

    @Test
    void postNetAcceptsOnlyCompleteLengths() throws Exception {
        PostNetEncoder encoder = new PostNetEncoder();
        assertNotNull(encoder.encode("8053659615"));   // 10: ZIP+4
        assertNotNull(encoder.encode("805365961563")); // 12: delivery point
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("80536"));     // 5
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("8053659"));   // 8
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("80536596"));  // 9
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("80536596156")); // 11
    }

    @Test
    void postNetFacadeAppendsCheckDigitToIncompleteInputs() throws Exception {
        BarcodeGenerator generator = BarcodeGenerator.of("POSTNET");
        assertNotNull(generator.toImage("805365961"));  // 9 digits + DV = 10
        assertNotNull(generator.toImage("80536"));      // 5 digits + DV = 6
        assertNotNull(generator.toImage("80536596157")); // 11 digits + DV = 12
    }

    // -------------------------------------------------------------------------------------------
    // UPC-E: number system 0/1 and check digit rules
    // -------------------------------------------------------------------------------------------

    @Test
    void upceRequiresNumberSystemZeroOrOne() throws Exception {
        UpceEncoder encoder = new UpceEncoder();
        String data = "0123456";
        String full = data + encoder.computeCheckSum(data);
        encoder.encode(full);
        String ns2 = "2123456" + encoder.computeCheckSum("2123456");
        assertThrows(InvalidSymbolException.class, () -> encoder.encode(ns2),
                "number system 2 is not a UPC-E number system");
    }

    @Test
    void upceCheckDigitIsEan13Based() throws Exception {
        // UPC-E checksum is computed over the UPC-A expansion; parity is covered by
        // ChecksumTest. Here: the check digit must be a single decimal digit.
        UpceEncoder encoder = new UpceEncoder();
        String checksum = encoder.computeCheckSum("0123456");
        assertEquals(1, checksum.length());
        assertTrue(checksum.charAt(0) >= '0' && checksum.charAt(0) <= '9',
                "check digit is a decimal digit");
    }

    // -------------------------------------------------------------------------------------------
    // UPC-A to UPC-E conversion
    // -------------------------------------------------------------------------------------------

    @Test
    void convertUPCAtoUPCERejectsNonCompressibleCodes() throws Exception {
        UpcaEncoder encoder = new UpcaEncoder();
        assertThrows(InvalidSymbolException.class,
                () -> encoder.convertUPCAtoUPCE("03456700012"),
                "manufacturer 34567, item 00012: I1I2=03 not compressible");
    }

    @Test
    void convertUPCAtoUPCEAcceptsCompressibleCodes() throws Exception {
        String compressed = new UpcaEncoder().convertUPCAtoUPCE("07567816415");
        assertNotNull(compressed);
        // The UPC-E symbol carries 6 data digits; the facade-facing value also embeds the
        // number system (7 chars: "0" + 6 digits) per the encoder contract.
        assertEquals(7, compressed.length());
        assertEquals('0', compressed.charAt(0), "number system 0");
    }

    @Test
    void convertUPCAtoUPCERejectsWrongLength() throws Exception {
        UpcaEncoder encoder = new UpcaEncoder();
        assertThrows(InvalidSymbolException.class, () -> encoder.convertUPCAtoUPCE("1234"));
        assertThrows(InvalidSymbolException.class, () -> encoder.convertUPCAtoUPCE(null));
    }

    // -------------------------------------------------------------------------------------------
    // Native encoder input validation
    // -------------------------------------------------------------------------------------------

    @Test
    void code128RejectsControlCharacters() throws Exception {
        Code128Encoder encoder = new Code128Encoder();
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("\u0001"));
        assertNotNull(encoder.encode("~"));
    }

    @Test
    void code39ExtRejectsNonLatin1() throws Exception {
        Code39ExtEncoder encoder = new Code39ExtEncoder();
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("\u20AC"));
    }

    @Test
    void code39ExtEncodesFullAsciiRange() throws Exception {
        Code39ExtEncoder encoder = new Code39ExtEncoder();
        // The full-ASCII table covers 0-127: lowercase letters map to "$+X" shift pairs.
        assertNotNull(encoder.encode("a"));
        assertNotNull(encoder.encode("{"));
        // Characters above 127 (e.g. "é") are not encodable in full-ASCII Code 39.
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("\u00E9"));
    }

    // -------------------------------------------------------------------------------------------
    // EAN-8 facade completion (the facade appends the check digit)
    // -------------------------------------------------------------------------------------------

    @Test
    void ean8FacadeCompletesTheCheckDigit() throws Exception {
        // 7 digits: the facade appends the check digit ("4" for 9638507).
        assertNotNull(BarcodeGenerator.of("EAN8").toImage("9638507"));
        // 8 digits: the supplied check digit is validated and kept.
        assertNotNull(BarcodeGenerator.of("EAN8").toImage("96385074"));
        assertThrows(Exception.class, () -> BarcodeGenerator.of("EAN8").toImage("96385075"));
        assertEquals(8, new Ean8Encoder().completeLength());
    }
}
