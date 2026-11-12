package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Checksum vectors for the native encoders: EAN/UPC assert known-correct standard values; the
 * remaining formats freeze the current behaviour so a change cannot happen silently.
 */
class ChecksumTest {

    private static void assertCheck(LinearEncoder encoder, String text, String expected) throws Exception {
        assertEquals(expected, encoder.computeCheckSum(text));
    }

    @Test
    @DisplayName("EAN-13 / EAN-8 / UPC-A against known-correct vectors")
    void eanUpcKnownVectors() throws Exception {
        assertCheck(new Ean13Encoder(), "400638133393", "1");
        assertCheck(new Ean13Encoder(), "789100031550", "7");
        assertCheck(new Ean8Encoder(), "9638507", "4");
        assertCheck(new UpcaEncoder(), "03600029145", "2");
    }

    @Test
    @DisplayName("UPC-E checksum")
    void upce() throws Exception {
        String upce = new UpcaEncoder().convertUPCAtoUPCE("07567816415");
        assertEquals("0756785", upce);
        assertCheck(new UpceEncoder(), upce, "8");
    }

    @Test
    @DisplayName("2 of 5 family checksum")
    void toF() throws Exception {
        assertCheck(new Standard2of5Encoder(), "978851500404", "1");
        assertCheck(new Iata2of5Encoder(), "978851500404", "1");
        assertCheck(new Interleaved2of5Encoder(), "987654321092", "0");
        assertCheck(new Matrix2of5Encoder(), "978851500404", "1");
        assertCheck(new Datalogic2of5Encoder(), "978851500404", "1");
    }

    @Test
    @DisplayName("ITF-14 checksum (GS1 GTIN-14)")
    void itf14() throws Exception {
        assertCheck(new Itf14Encoder(), "1540014128876", "3");
    }

    @Test
    @DisplayName("Code 11 / MSI / Code 39 / Code 93 / Code 128 / Codabar / EAN-5 checksum")
    void codes() throws Exception {
        assertCheck(new Code11Encoder(), "9788515004041", "03");
        assertCheck(new MsiPlesseyEncoder(), "9788515004041", "6");
        assertCheck(new Code39Encoder(), "BARS4J-39", "I");
        assertCheck(new Code39ExtEncoder(), "BARS4J-39", "I");
        assertCheck(new Code93Encoder(), "BARS4J-93", "9O");
        assertCheck(new Code93ExtEncoder(), "BARS4J-93", "9O");
        assertCheck(new Code128Encoder(), "Bars4J-128", "");
        assertCheck(new CodabarEncoder(), "97885150040-85", "");
        assertCheck(new Ean5Encoder(), "54495", "9");
    }

    @Test
    @DisplayName("PostNet checksum")
    void postNet() throws Exception {
        assertCheck(new PostNetEncoder(), "805365961", "7");
    }

    /**
     * The PostNet checksum must be "0" when the digit sum is a multiple of 10 (it used to
     * return "10").
     */
    @Test
    @DisplayName("PostNet checksum is 0 on zero remainder")
    void postNetZeroRemainder() throws Exception {
        assertCheck(new PostNetEncoder(), "1234", "0");
        assertCheck(new PostNetEncoder(), "19", "0");
    }
}
