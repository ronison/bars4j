package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.bars4j.api.BarcodeGenerator;
import org.bars4j.encode.UPCAEncoder;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * The unified registry/generator API must reproduce the legacy 1D raster output (including the
 * human-readable text) byte-for-byte for every supported format.
 */
class GeneratorImageMatchesLegacyTest {

    private record Case(String id, Supplier<Barcode> factory, String code) {
    }

    private static List<Case> cases() throws Exception {
        List<Case> cases = new ArrayList<>();
        cases.add(new Case("EAN13", () -> BarcodeFactory.getInstance().createEAN13(), "789100031550"));
        cases.add(new Case("EAN8", () -> BarcodeFactory.getInstance().createEAN8(), "9788515"));
        cases.add(new Case("EAN5", () -> BarcodeFactory.getInstance().createEAN5(), "54495"));
        cases.add(new Case("UPCA", () -> BarcodeFactory.getInstance().createUPCA(), "07567816415"));
        cases.add(new Case("UPCE", () -> BarcodeFactory.getInstance().createUPCE(),
                UPCAEncoder.getInstance().convertUPCAtoUPCE("07567816415")));
        cases.add(new Case("CODABAR", () -> BarcodeFactory.getInstance().createCodabar(), "97885150040-85"));
        cases.add(new Case("CODE39", () -> BarcodeFactory.getInstance().createCode39(), "BARS4J-39"));
        cases.add(new Case("CODE39EXT", () -> BarcodeFactory.getInstance().createCode39Extended(), "Bars4J-39Ext"));
        cases.add(new Case("CODE93", () -> BarcodeFactory.getInstance().createCode93(), "BARS4J-93"));
        cases.add(new Case("CODE93EXT", () -> BarcodeFactory.getInstance().createCode93Extended(), "Bars4J-93Ext"));
        cases.add(new Case("CODE128", () -> BarcodeFactory.getInstance().createCode128(), "Bars4J-128"));
        cases.add(new Case("CODE11", () -> BarcodeFactory.getInstance().createCode11(), "9788515004041"));
        cases.add(new Case("MSIPLESSEY", () -> BarcodeFactory.getInstance().createMSIPlessey(), "9788515004041"));
        cases.add(new Case("STANDARD2OF5", () -> BarcodeFactory.getInstance().createStandard2of5(), "978851500404"));
        cases.add(new Case("IATA2OF5", () -> BarcodeFactory.getInstance().createIATA2of5(), "978851500404"));
        cases.add(new Case("MATRIX2OF5", () -> BarcodeFactory.getInstance().createMatrix2of5(), "123456"));
        cases.add(new Case("DATALOGIC2OF5", () -> BarcodeFactory.getInstance().createDatalogic2of5(), "123456"));
        cases.add(new Case("INTERLEAVED2OF5", () -> BarcodeFactory.getInstance().createInterleaved2of5(), "9876543210921"));
        cases.add(new Case("POSTNET", () -> BarcodeFactory.getInstance().createPostNet(), "805365961"));
        return cases;
    }

    @TestFactory
    Stream<DynamicTest> imageMatchesLegacy() throws Exception {
        return cases().stream().map(c -> dynamicTest(c.id(), () -> {
            BufferedImage expected = c.factory().get().createBarcode(c.code());
            BufferedImage actual = BarcodeGenerator.of(c.id()).toImage(c.code());
            assertImagesEqual(expected, actual, c.id());
        }));
    }

    private static void assertImagesEqual(BufferedImage expected, BufferedImage actual, String name) {
        assertEquals(expected.getWidth(), actual.getWidth(), "width " + name);
        assertEquals(expected.getHeight(), actual.getHeight(), "height " + name);
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                assertEquals(expected.getRGB(x, y), actual.getRGB(x, y), "pixel " + x + "," + y + " " + name);
            }
        }
    }
}
