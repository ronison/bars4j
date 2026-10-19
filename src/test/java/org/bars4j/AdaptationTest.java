package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.bars4j.encode.BarSet;
import org.bars4j.encode.BarcodeEncoder;
import org.bars4j.encode.CodabarEncoder;
import org.bars4j.encode.Code11Encoder;
import org.bars4j.encode.Code128Encoder;
import org.bars4j.encode.Code39Encoder;
import org.bars4j.encode.Code39ExtEncoder;
import org.bars4j.encode.Code93Encoder;
import org.bars4j.encode.Code93ExtEncoder;
import org.bars4j.encode.Datalogic2of5Encoder;
import org.bars4j.encode.EAN13Encoder;
import org.bars4j.encode.EAN5Encoder;
import org.bars4j.encode.EAN8Encoder;
import org.bars4j.encode.IATA2of5Encoder;
import org.bars4j.encode.Interleaved2of5Encoder;
import org.bars4j.encode.MSIPlesseyEncoder;
import org.bars4j.encode.Matrix2of5Encoder;
import org.bars4j.encode.PostNetEncoder;
import org.bars4j.encode.Standard2of5Encoder;
import org.bars4j.encode.UPCAEncoder;
import org.bars4j.encode.UPCEEncoder;
import org.bars4j.encode.linear.LegacyLinearEncoderAdapter;
import org.bars4j.format.BitSemantics;
import org.bars4j.paint.BarcodePainter;
import org.bars4j.paint.HeightCodedPainter;
import org.bars4j.paint.WideRatioCodedPainter;
import org.bars4j.paint.WidthCodedPainter;
import org.bars4j.render.BarcodeRenderer;
import org.bars4j.render.RenderSpec;
import org.bars4j.render.linear.LegacyPainterRenderer;
import org.bars4j.symbol.LinearSymbol;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Phase 1: verifies that the new symbol/renderer architecture reproduces the legacy bit arrays
 * and painter output exactly.
 */
class AdaptationTest {

    private record Case(String name, BarcodeEncoder encoder, BarcodePainter painter,
                        BitSemantics semantics, String code, boolean needsCheck) {
        LinearSymbol symbol() throws Exception {
            return new LegacyLinearEncoderAdapter(encoder, semantics).encode(effectiveCode());
        }

        BarcodeRenderer renderer() {
            return new LegacyPainterRenderer(painter, semantics);
        }

        /** The facade appends the check digit before encoding; replicate that here. */
        String effectiveCode() throws Exception {
            return needsCheck ? code + encoder.computeCheckSum(code) : code;
        }
    }

    private static final RenderSpec SPEC = RenderSpec.linearDefaults();

    private static List<Case> cases() throws Exception {
        List<Case> cases = new ArrayList<>();
        cases.add(new Case("EAN13", EAN13Encoder.getInstance(), WidthCodedPainter.getInstance(),
                BitSemantics.MODULE_DARK, "789100031550", true));
        cases.add(new Case("EAN8", EAN8Encoder.getInstance(), WidthCodedPainter.getInstance(),
                BitSemantics.MODULE_DARK, "9788515", true));
        cases.add(new Case("EAN5", EAN5Encoder.getInstance(), WidthCodedPainter.getInstance(),
                BitSemantics.MODULE_DARK, "54495", false));
        cases.add(new Case("UPCA", UPCAEncoder.getInstance(), WidthCodedPainter.getInstance(),
                BitSemantics.MODULE_DARK, "07567816415", true));
        cases.add(new Case("UPCE", UPCEEncoder.getInstance(), WidthCodedPainter.getInstance(),
                BitSemantics.MODULE_DARK, UPCAEncoder.getInstance().convertUPCAtoUPCE("07567816415"), true));
        cases.add(new Case("Codabar", CodabarEncoder.getInstance(), WideRatioCodedPainter.getInstance(),
                BitSemantics.WIDE_NARROW_ALTERNATING, "97885150040-85", false));
        cases.add(new Case("Code39", Code39Encoder.getInstance(), WideRatioCodedPainter.getInstance(),
                BitSemantics.WIDE_NARROW_ALTERNATING, "BARS4J-39", false));
        cases.add(new Case("Code39Ext", Code39ExtEncoder.getInstance(), WideRatioCodedPainter.getInstance(),
                BitSemantics.WIDE_NARROW_ALTERNATING, "Bars4J-39Ext", false));
        cases.add(new Case("Code93", Code93Encoder.getInstance(), WidthCodedPainter.getInstance(),
                BitSemantics.MODULE_DARK, "BARS4J-93", true));
        cases.add(new Case("Code93Ext", Code93ExtEncoder.getInstance(), WidthCodedPainter.getInstance(),
                BitSemantics.MODULE_DARK, "Bars4J-93Ext", true));
        cases.add(new Case("Code128", Code128Encoder.getInstance(), WidthCodedPainter.getInstance(),
                BitSemantics.MODULE_DARK, "Bars4J-128", true));
        cases.add(new Case("Code11", Code11Encoder.getInstance(), WidthCodedPainter.getInstance(),
                BitSemantics.MODULE_DARK, "9788515004041", true));
        cases.add(new Case("MSIPlessey", MSIPlesseyEncoder.getInstance(), WidthCodedPainter.getInstance(),
                BitSemantics.MODULE_DARK, "9788515004041", true));
        cases.add(new Case("Standard2of5", Standard2of5Encoder.getInstance(), WideRatioCodedPainter.getInstance(),
                BitSemantics.WIDE_NARROW_ALTERNATING, "978851500404", true));
        cases.add(new Case("IATA2of5", IATA2of5Encoder.getInstance(), WideRatioCodedPainter.getInstance(),
                BitSemantics.WIDE_NARROW_ALTERNATING, "978851500404", true));
        cases.add(new Case("Matrix2of5", Matrix2of5Encoder.getInstance(), WideRatioCodedPainter.getInstance(),
                BitSemantics.WIDE_NARROW_ALTERNATING, "123456", true));
        cases.add(new Case("Datalogic2of5", Datalogic2of5Encoder.getInstance(), WideRatioCodedPainter.getInstance(),
                BitSemantics.WIDE_NARROW_ALTERNATING, "123456", true));
        cases.add(new Case("Interleaved2of5", Interleaved2of5Encoder.getInstance(), WideRatioCodedPainter.getInstance(),
                BitSemantics.WIDE_NARROW_ALTERNATING, "9876543210921", true));
        cases.add(new Case("PostNet", PostNetEncoder.getInstance(), HeightCodedPainter.getInstance(),
                BitSemantics.HEIGHT_CODED, "805365961", true));
        return cases;
    }

    private static String bits(BarSet[] bars) {
        StringBuilder sb = new StringBuilder();
        for (BarSet bar : bars) {
            sb.append(bar.toString());
        }
        return sb.toString();
    }

    @TestFactory
    Stream<DynamicTest> legacyBitsArePreserved() throws Exception {
        return cases().stream().map(c -> dynamicTest(c.name(), () -> {
            BarSet[] legacy = c.encoder().encode(c.effectiveCode());
            LinearSymbol symbol = c.symbol();
            BarSet[] roundTrip = org.bars4j.encode.linear.BarSetBridge.toBarSets(symbol, c.semantics());
            assertEquals(bits(legacy), bits(roundTrip), "bit array changed for " + c.name());
        }));
    }

    @TestFactory
    Stream<DynamicTest> rendererMatchesLegacyPainter() throws Exception {
        return cases().stream().map(c -> dynamicTest(c.name(), () -> {
            BarSet[] legacy = c.encoder().encode(c.effectiveCode());
            BufferedImage expected = c.painter().paintToImg(legacy, SPEC.barWidth(), SPEC.barHeight(), SPEC.wideRatio());
            BufferedImage actual = c.renderer().toImage(c.symbol(), SPEC);
            assertImagesEqual(expected, actual, c.name());

            String expectedSvg = c.painter().paintToSVG(legacy, SPEC.barWidth(), SPEC.barHeight(), SPEC.wideRatio()).toString();
            String actualSvg = c.renderer().toSvg(c.symbol(), SPEC);
            assertEquals(expectedSvg, actualSvg, "SVG changed for " + c.name());
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
