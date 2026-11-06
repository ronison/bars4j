package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Stream;

import org.bars4j.api.BarcodeGenerator;
import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.encode.linear.Ean13Encoder;
import org.bars4j.encode.linear.Ean8Encoder;
import org.bars4j.encode.linear.UpcaEncoder;
import org.bars4j.encode.linear.UpceEncoder;
import org.bars4j.format.BarcodeRegistry;
import org.bars4j.format.LinearBarcodeFormat;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * The EAN/UPC encoders must validate a check digit that is already part of the supplied code
 * (instead of blindly appending another one), rejecting a wrong check digit with a checked
 * {@link InvalidSymbolException}.
 *
 * <p>Known-correct vectors (standard EAN/UPC payloads with their computed check digit):
 *
 * <ul>
 *   <li>EAN-13: {@code 789100031550} + {@code 7} = {@code 7891000315507}</li>
 *   <li>EAN-8: {@code 9788515} + {@code 3} = {@code 97885153}</li>
 *   <li>UPC-A: {@code 03600029145} + {@code 2} = {@code 036000291452}</li>
 *   <li>UPC-E: {@code 0756785} + {@code 8} = {@code 07567858}</li>
 * </ul>
 */
class CheckDigitValidationTest {

    private record Case(String id, String data, String check, LinearEncoder encoder, int completeLength) {
        String complete() {
            return data + check;
        }

        /** Same payload with a deliberately wrong check digit. */
        String wrong() {
            return data + (check.equals("0") ? "1" : "0");
        }
    }

    private static List<Case> cases() {
        return List.of(
                new Case("EAN13", "789100031550", "7", new Ean13Encoder(), 13),
                new Case("EAN8", "9788515", "3", new Ean8Encoder(), 8),
                new Case("UPCA", "03600029145", "2", new UpcaEncoder(), 12),
                new Case("UPCE", "0756785", "8", new UpceEncoder(), 8));
    }

    @TestFactory
    Stream<DynamicTest> encodersExposeCompleteLength() {
        return cases().stream().map(c -> dynamicTest(c.id(), () ->
                assertEquals(c.completeLength(), c.encoder().completeLength())));
    }

    @TestFactory
    Stream<DynamicTest> rawEncoderAcceptsValidCheckDigit() {
        return cases().stream().map(c -> dynamicTest(c.id(), () ->
                assertNotNull(c.encoder().encode(c.complete()))));
    }

    @TestFactory
    Stream<DynamicTest> rawEncoderRejectsWrongCheckDigit() {
        return cases().stream().map(c -> dynamicTest(c.id(), () ->
                assertThrows(InvalidSymbolException.class, () -> c.encoder().encode(c.wrong()))));
    }

    @TestFactory
    Stream<DynamicTest> facadeAcceptsValidCheckDigitWithDefaultOptions() {
        return cases().stream().map(c -> dynamicTest(c.id(), () -> {
            BufferedImage image = BarcodeGenerator.of(c.id()).toImage(c.complete());
            assertNotNull(image);
        }));
    }

    @TestFactory
    Stream<DynamicTest> facadeRejectsWrongCheckDigitWithDefaultOptions() {
        return cases().stream().map(c -> dynamicTest(c.id(), () ->
                assertThrows(InvalidSymbolException.class, () -> BarcodeGenerator.of(c.id()).toImage(c.wrong()))));
    }

    @TestFactory
    Stream<DynamicTest> facadeAcceptsValidCheckDigitWhenAppendingIsDisabled() {
        return cases().stream().map(c -> dynamicTest(c.id(), () -> {
            LinearBarcodeFormat noCheck = withoutAppending(c.id());
            assertNotNull(noCheck.toImage(c.complete(), org.bars4j.render.RenderSpec.linearDefaults()));
            assertThrows(InvalidSymbolException.class,
                    () -> noCheck.toImage(c.wrong(), org.bars4j.render.RenderSpec.linearDefaults()));
        }));
    }

    /** Returns the format with the automatic check-digit appending disabled. */
    private static LinearBarcodeFormat withoutAppending(String id) {
        LinearBarcodeFormat base = (LinearBarcodeFormat) BarcodeRegistry.getInstance().get(id);
        LinearBarcodeFormat.LinearOptions o = base.options();
        return base.withOptions(new LinearBarcodeFormat.LinearOptions(
                o.xDimensionMm(), o.barHeightMm(), o.wideRatio(), false, o.showCheckDigit(), o.showText()));
    }
}
