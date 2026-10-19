package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bars4j.encode.BarcodeEncoder;
import org.bars4j.encode.Code11Encoder;
import org.bars4j.encode.Code128Encoder;
import org.bars4j.encode.Code39Encoder;
import org.bars4j.encode.Code93Encoder;
import org.bars4j.encode.EAN13Encoder;
import org.bars4j.encode.EAN8Encoder;
import org.bars4j.encode.IATA2of5Encoder;
import org.bars4j.encode.Interleaved2of5Encoder;
import org.bars4j.encode.MSIPlesseyEncoder;
import org.bars4j.encode.PostNetEncoder;
import org.bars4j.encode.Standard2of5Encoder;
import org.bars4j.encode.UPCAEncoder;
import org.bars4j.encode.UPCEEncoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Phase 0 checksum characterization tests.
 *
 * <p>Known-correct vectors (EAN/UPC) assert standard values; the remaining formats freeze the
 * current behaviour so the refactoring cannot change it silently.
 */
class ChecksumTest {

    private static void assertCheck(BarcodeEncoder encoder, String text, String expected) throws Exception {
        assertEquals(expected, encoder.computeCheckSum(text));
    }

    @Test
    @DisplayName("EAN-13 / EAN-8 / UPC-A against known-correct vectors")
    void eanUpcKnownVectors() throws Exception {
        assertCheck(EAN13Encoder.getInstance(), "400638133393", "1");
        assertCheck(EAN13Encoder.getInstance(), "789100031550", "7");
        assertCheck(EAN8Encoder.getInstance(), "9638507", "4");
        assertCheck(UPCAEncoder.getInstance(), "03600029145", "2");
    }

    @Test
    @DisplayName("UPCE checksum (characterization)")
    void upce() throws Exception {
        String upce = UPCAEncoder.getInstance().convertUPCAtoUPCE("07567816415");
        assertEquals("0756785", upce);
        assertCheck(UPCEEncoder.getInstance(), upce, "8");
    }

    @Test
    @DisplayName("2 of 5 family checksum (characterization)")
    void toF() throws Exception {
        assertCheck(Standard2of5Encoder.getInstance(), "978851500404", "1");
        assertCheck(IATA2of5Encoder.getInstance(), "978851500404", "1");
        assertCheck(Interleaved2of5Encoder.getInstance(), "987654321092", "0");
    }

    @Test
    @DisplayName("Code 11 / MSI / Code 39 / Code 93 / Code 128 checksum (characterization)")
    void codes() throws Exception {
        assertCheck(Code11Encoder.getInstance(), "9788515004041", "03");
        assertCheck(MSIPlesseyEncoder.getInstance(), "9788515004041", "6");
        assertCheck(Code39Encoder.getInstance(), "BARS4J-39", "I");
        assertCheck(Code93Encoder.getInstance(), "BARS4J-93", "9O");
        assertCheck(Code128Encoder.getInstance(), "Bars4J-128", "");
    }

    @Test
    @DisplayName("PostNet checksum (characterization)")
    void postNet() throws Exception {
        assertCheck(PostNetEncoder.getInstance(), "805365961", "7");
    }

    /**
     * Regression test for the previously buggy PostNet checksum: when the digit sum is a multiple
     * of 10 the checksum must be "0" (it used to return "10").
     */
    @Test
    @DisplayName("PostNet checksum is 0 on zero remainder")
    void postNetZeroRemainder() throws Exception {
        assertCheck(PostNetEncoder.getInstance(), "1234", "0");
        assertCheck(PostNetEncoder.getInstance(), "19", "0");
    }
}
