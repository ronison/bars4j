package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.bars4j.api.BarcodeGenerator;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Phase 2: the unified registry/generator API must reproduce the legacy 1D SVG output
 * (verified against the Phase 0 goldens).
 */
class BarcodeGeneratorGoldenTest {

    private record Case(String id, String goldenName, String code) {
    }

    private static final Case[] CASES = {
            new Case("EAN13", "EAN13", "789100031550"),
            new Case("EAN8", "EAN8", "9788515"),
            new Case("EAN5", "EAN5", "54495"),
            new Case("UPCA", "UPCA", "07567816415"),
            new Case("UPCE", "UPCE", "0756785"),
            new Case("CODABAR", "Codabar", "97885150040-85"),
            new Case("CODE39", "Code39", "BARS4J-39"),
            new Case("CODE39EXT", "Code39Ext", "Bars4J-39Ext"),
            new Case("CODE93", "Code93", "BARS4J-93"),
            new Case("CODE93EXT", "Code93Ext", "Bars4J-93Ext"),
            new Case("CODE128", "Code128", "Bars4J-128"),
            new Case("CODE11", "Code11", "9788515004041"),
            new Case("MSIPLESSEY", "MSIPlessey", "9788515004041"),
            new Case("STANDARD2OF5", "Standard2of5", "978851500404"),
            new Case("IATA2OF5", "IATA2of5", "978851500404"),
            new Case("MATRIX2OF5", "Matrix2of5", "123456"),
            new Case("DATALOGIC2OF5", "Datalogic2of5", "123456"),
            new Case("INTERLEAVED2OF5", "Interleaved2of5", "9876543210921"),
            new Case("POSTNET", "PostNet", "805365961"),
    };

    @TestFactory
    Stream<DynamicTest> generatorMatchesGoldenSvg() {
        return Stream.of(CASES).map(c -> dynamicTest(c.id(), () -> {
            String actual = BarcodeGenerator.of(c.id()).toSvg(c.code());
            String expected = Files.readString(
                    Path.of("src", "test", "resources", "golden", c.goldenName() + ".svg"));
            assertEquals(expected, actual, "SVG mismatch for " + c.id());
        }));
    }
}
