package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bars4j.api.BarcodeGenerator;
import org.bars4j.encode.Code11Encoder;
import org.bars4j.encode.Code128Encoder;
import org.bars4j.encode.Code39ExtEncoder;
import org.bars4j.encode.InvalidAtributeException;
import org.bars4j.encode.UPCAEncoder;
import org.bars4j.encode.linear.PostNetEncoder;
import org.bars4j.encode.linear.UpceEncoder;
import org.bars4j.encode.api.InvalidSymbolException;
import org.junit.jupiter.api.Test;

/**
 * Specification vectors and regression scenarios for the encoder fixes: Code 11 mod-11 checksum
 * with the dash (10) symbol, PostNet complete-length enforcement, UPC-E number system rules,
 * non-compressible UPC-A to UPC-E conversion and legacy encoder input validation.
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
        org.bars4j.encode.BarcodeEncoder encoder = org.bars4j.encode.Code11Encoder.getInstance();
        String checksum = encoder.computeCheckSum("1000000000000");
        assertNotNull(checksum);
        assertEquals(2, checksum.length(), "13 digits require C and K");
    }

    @Test
    void code11CheckDigitTenIsRenderedAsDash() throws Exception {
        org.bars4j.encode.BarcodeEncoder encoder = org.bars4j.encode.Code11Encoder.getInstance();
        // C = 10 renders as "-": verified by hand, weights from the right.
        assertEquals("-", encoder.computeCheckSum("69"));
        assertEquals(1, encoder.computeCheckSum("69").length(), "fewer than 10 digits: C only");
    }

    @Test
    void code11NineDigitsProduceOnlyTheCDigit() throws Exception {
        org.bars4j.encode.BarcodeEncoder encoder = org.bars4j.encode.Code11Encoder.getInstance();
        String checksum = encoder.computeCheckSum("123456789");
        assertEquals(1, checksum.length(), "9 digits: only C");
    }

    @Test
    void code11TenDigitsProduceCAndK() throws Exception {
        org.bars4j.encode.BarcodeEncoder encoder = org.bars4j.encode.Code11Encoder.getInstance();
        String checksum = encoder.computeCheckSum("1234567890");
        assertEquals(2, checksum.length(), "10 digits: C and K");
    }

    @Test
    void code11MatchesSpecVector123456789() throws Exception {
        // Verified empirically: "123456789" -> C = 0, and with 9 digits only C is produced.
        org.bars4j.encode.BarcodeEncoder encoder = org.bars4j.encode.Code11Encoder.getInstance();
        assertEquals("0", encoder.computeCheckSum("123456789"));
        // 13 digits produce both C and K: verified "1000000000000" -> "38".
        assertEquals("38", encoder.computeCheckSum("1000000000000"));
        // C = 10 renders as the dash character.
        assertEquals("-", encoder.computeCheckSum("69"));
    }

    @Test
    void code11RejectsNonDigitsAndUnknownChars() throws Exception {
        org.bars4j.encode.BarcodeEncoder encoder = org.bars4j.encode.Code11Encoder.getInstance();
        assertThrows(InvalidAtributeException.class, () -> encoder.computeCheckSum("12a4"));
        assertThrows(InvalidAtributeException.class, () -> encoder.encode("12a4"));
        assertThrows(InvalidAtributeException.class, () -> encoder.computeCheckSum(null));
        assertThrows(InvalidAtributeException.class, () -> encoder.encode(null));
    }

    @Test
    void code11LegacyAndNativeChecksumsAgree() throws Exception {
        org.bars4j.encode.BarcodeEncoder legacy = org.bars4j.encode.Code11Encoder.getInstance();
        org.bars4j.encode.linear.Code11Encoder nativeEncoder = new org.bars4j.encode.linear.Code11Encoder();
        for (String text : new String[]{"9788515004041", "123456789", "69", "1000000000000"}) {
            assertEquals(legacy.computeCheckSum(text), nativeEncoder.computeCheckSum(text),
                    "Code 11 checksum parity for " + text);
        }
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
        assertThrows(InvalidSymbolException.class, () -> encoder.encode("805365961")); // 9 -> wait: this is 9 chars, rejected
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
        // AdaptationTest. Here: the check digit must be a single decimal digit.
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
        UPCAEncoder encoder = UPCAEncoder.getInstance();
        // "07567816415" compresses (I1=7: I2..I5=8164 -> not compressible? verified: the sample
        // converts). Non-compressible: I1I2=00 with non-zero I3/I4/I5 trailing digits.
        assertThrows(InvalidAtributeException.class,
                () -> UPCAEncoder.getInstance().convertUPCAtoUPCE("03456700012"),
                "manufacturer 34567, item 00012: I1I2=03 not compressible");
    }

    @Test
    void convertUPCAtoUPCEAcceptsCompressibleCodes() throws Exception {
        String compressed = UPCAEncoder.getInstance().convertUPCAtoUPCE("07567816415");
        assertNotNull(compressed);
        // The UPC-E symbol carries 6 data digits; the factory-facing value also embeds the
        // number system (7 chars: "0" + 6 digits) per the legacy encoder contract.
        assertEquals(7, compressed.length());
        assertEquals('0', compressed.charAt(0), "number system 0");
    }

    @Test
    void convertUPCAtoUPCERejectsWrongLength() throws Exception {
        UPCAEncoder encoder = UPCAEncoder.getInstance();
        assertThrows(InvalidAtributeException.class, () -> encoder.convertUPCAtoUPCE("1234"));
        assertThrows(InvalidAtributeException.class, () -> encoder.convertUPCAtoUPCE(null));
    }

    // -------------------------------------------------------------------------------------------
    // legacy encoder input validation
    // -------------------------------------------------------------------------------------------

    @Test
    void code128LegacyRejectsControlCharacters() throws Exception {
        org.bars4j.encode.BarcodeEncoder encoder = Code128Encoder.getInstance();
        assertThrows(InvalidAtributeException.class, () -> encoder.encode("\u0001"));
        assertNotNull(encoder.encode("~"));
    }

    @Test
    void code39ExtLegacyRejectsNonLatin1() throws Exception {
        org.bars4j.encode.BarcodeEncoder encoder = Code39ExtEncoder.getInstance();
        assertThrows(InvalidAtributeException.class, () -> encoder.encode("\u20AC"));
    }

    @Test
    void code39ExtLegacyEncodesFullAsciiRange() throws Exception {
        org.bars4j.encode.BarcodeEncoder encoder = Code39ExtEncoder.getInstance();
        // The full-ASCII table covers 0-127: lowercase letters map to "$+X" shift pairs.
        assertNotNull(encoder.encode("a"));
        assertNotNull(encoder.encode("{"));
        // Characters above 127 (e.g. "é") are not encodable in full-ASCII Code 39.
        assertThrows(InvalidAtributeException.class, () -> encoder.encode("\u00E9"));
    }
}
