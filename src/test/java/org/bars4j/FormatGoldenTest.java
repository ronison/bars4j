package org.bars4j;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.bars4j.encode.UPCAEncoder;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Phase 0 characterization tests: freeze the current SVG and bar-geometry output of every
 * supported 1D format before the architecture refactoring starts.
 */
class FormatGoldenTest {

    private record Case(String name, Supplier<Barcode> factory, String code) {
    }

    private static List<Case> cases() throws Exception {
        List<Case> cases = new ArrayList<>();
        cases.add(new Case("EAN13", () -> BarcodeFactory.getInstance().createEAN13(), "789100031550"));
        cases.add(new Case("EAN8", () -> BarcodeFactory.getInstance().createEAN8(), "9788515"));
        cases.add(new Case("EAN5", () -> BarcodeFactory.getInstance().createEAN5(), "54495"));
        cases.add(new Case("UPCA", () -> BarcodeFactory.getInstance().createUPCA(), "07567816415"));
        cases.add(new Case("UPCE", () -> BarcodeFactory.getInstance().createUPCE(),
                UPCAEncoder.getInstance().convertUPCAtoUPCE("07567816415")));
        cases.add(new Case("Codabar", () -> BarcodeFactory.getInstance().createCodabar(), "97885150040-85"));
        cases.add(new Case("Code39", () -> BarcodeFactory.getInstance().createCode39(), "BARS4J-39"));
        cases.add(new Case("Code39Ext", () -> BarcodeFactory.getInstance().createCode39Extended(), "Bars4J-39Ext"));
        cases.add(new Case("Code93", () -> BarcodeFactory.getInstance().createCode93(), "BARS4J-93"));
        cases.add(new Case("Code93Ext", () -> BarcodeFactory.getInstance().createCode93Extended(), "Bars4J-93Ext"));
        cases.add(new Case("Code128", () -> BarcodeFactory.getInstance().createCode128(), "Bars4J-128"));
        cases.add(new Case("Code11", () -> BarcodeFactory.getInstance().createCode11(), "9788515004041"));
        cases.add(new Case("MSIPlessey", () -> BarcodeFactory.getInstance().createMSIPlessey(), "9788515004041"));
        cases.add(new Case("Standard2of5", () -> BarcodeFactory.getInstance().createStandard2of5(), "978851500404"));
        cases.add(new Case("IATA2of5", () -> BarcodeFactory.getInstance().createIATA2of5(), "978851500404"));
        cases.add(new Case("Matrix2of5", () -> BarcodeFactory.getInstance().createMatrix2of5(), "123456"));
        cases.add(new Case("Datalogic2of5", () -> BarcodeFactory.getInstance().createDatalogic2of5(), "123456"));
        cases.add(new Case("Interleaved2of5", () -> BarcodeFactory.getInstance().createInterleaved2of5(), "9876543210921"));
        cases.add(new Case("PostNet", () -> BarcodeFactory.getInstance().createPostNet(), "805365961"));
        return cases;
    }

    @TestFactory
    Stream<DynamicTest> svgGolden() throws Exception {
        return cases().stream().map(c -> dynamicTest(c.name() + "-svg", () -> {
            String svg = GoldenTestSupport.svgOf(c.factory().get(), c.code());
            GoldenTestSupport.assertSvg(c.name(), svg);
        }));
    }

    @TestFactory
    Stream<DynamicTest> imageGolden() throws Exception {
        return cases().stream().map(c -> dynamicTest(c.name() + "-image", () -> {
            GoldenTestSupport.assertImage(c.name(), GoldenTestSupport.imageOf(c.factory().get(), c.code()));
        }));
    }
}
