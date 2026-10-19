package org.bars4j.format;

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
import org.bars4j.encode.linear.Matrix2of5Encoder;
import org.bars4j.encode.linear.MsiPlesseyEncoder;
import org.bars4j.encode.linear.PostNetEncoder;
import org.bars4j.encode.linear.Standard2of5Encoder;
import org.bars4j.encode.linear.UpcaEncoder;
import org.bars4j.encode.linear.UpceEncoder;
import org.bars4j.render.linear.BaseLineTextRenderer;
import org.bars4j.render.linear.Ean13TextRenderer;
import org.bars4j.render.linear.Ean8TextRenderer;
import org.bars4j.render.linear.HeightCodedRenderer;
import org.bars4j.render.linear.LinearRenderer;
import org.bars4j.render.linear.LinearTextRenderer;
import org.bars4j.render.linear.UpcaTextRenderer;
import org.bars4j.render.linear.UpceTextRenderer;
import org.bars4j.render.linear.WideRatioCodedRenderer;
import org.bars4j.render.linear.WidthCodedRenderer;

/**
 * Registers the formats shipped with the library.
 */
final class BuiltinFormats {

    /** x-dimension used by the EAN/UPC/Code11/MSI/Matrix/Datalogic formats (1 module). */
    private static final double XD_1 = 0.264583333;
    /** x-dimension used by the wide-ratio and larger formats (2 modules). */
    private static final double XD_2 = 0.5291666;

    private BuiltinFormats() {
    }

    static void register(BarcodeRegistry registry) {
        registry.register(linear("EAN13", "EAN-13", new Ean13Encoder(), new WidthCodedRenderer(),
                new Ean13TextRenderer(), XD_1, 19, 2.0, true, true, true));
        registry.register(linear("EAN8", "EAN-8", new Ean8Encoder(), new WidthCodedRenderer(),
                new Ean8TextRenderer(), XD_1, 19, 2.0, true, true, true));
        registry.register(linear("EAN5", "EAN-5", new Ean5Encoder(), new WidthCodedRenderer(),
                new BaseLineTextRenderer(), XD_1, 17, 2.0, false, false, true));
        registry.register(linear("UPCA", "UPC-A", new UpcaEncoder(), new WidthCodedRenderer(),
                new UpcaTextRenderer(), XD_1, 17, 2.0, true, true, true));
        registry.register(linear("UPCE", "UPC-E", new UpceEncoder(), new WidthCodedRenderer(),
                new UpceTextRenderer(), XD_1, 17, 2.0, true, true, true));
        registry.register(linear("CODABAR", "Codabar", new CodabarEncoder(), new WideRatioCodedRenderer(),
                new BaseLineTextRenderer(), XD_2, 17, 3.0, false, false, true));
        registry.register(linear("CODE39", "Code 39", new Code39Encoder(), new WideRatioCodedRenderer(),
                new BaseLineTextRenderer(), XD_2, 17, 2.0, false, false, true));
        registry.register(linear("CODE39EXT", "Code 39 Extended", new Code39ExtEncoder(), new WideRatioCodedRenderer(),
                new BaseLineTextRenderer(), XD_2, 17, 2.0, false, false, true));
        registry.register(linear("CODE93", "Code 93", new Code93Encoder(), new WidthCodedRenderer(),
                new BaseLineTextRenderer(), XD_2, 17, 2.0, true, false, true));
        registry.register(linear("CODE93EXT", "Code 93 Extended", new Code93ExtEncoder(), new WidthCodedRenderer(),
                new BaseLineTextRenderer(), XD_2, 17, 2.0, true, false, true));
        registry.register(linear("CODE128", "Code 128", new Code128Encoder(), new WidthCodedRenderer(),
                new BaseLineTextRenderer(), XD_2, 17, 2.0, true, false, true));
        registry.register(linear("CODE11", "Code 11", new Code11Encoder(), new WidthCodedRenderer(),
                new BaseLineTextRenderer(), XD_2, 17, 2.0, true, true, true));
        registry.register(linear("MSIPLESSEY", "MSI Plessey", new MsiPlesseyEncoder(), new WidthCodedRenderer(),
                new BaseLineTextRenderer(), XD_1, 17, 2.0, true, true, true));
        registry.register(linear("STANDARD2OF5", "Standard 2 of 5", new Standard2of5Encoder(), new WideRatioCodedRenderer(),
                new BaseLineTextRenderer(), XD_2, 17, 2.0, true, false, true));
        registry.register(linear("IATA2OF5", "IATA 2 of 5", new Iata2of5Encoder(), new WideRatioCodedRenderer(),
                new BaseLineTextRenderer(), XD_2, 17, 2.0, true, false, true));
        registry.register(linear("INTERLEAVED2OF5", "Interleaved 2 of 5", new Interleaved2of5Encoder(), new WideRatioCodedRenderer(),
                new BaseLineTextRenderer(), XD_2, 17, 2.0, true, false, true));
        registry.register(linear("MATRIX2OF5", "Matrix 2 of 5", new Matrix2of5Encoder(), new WideRatioCodedRenderer(),
                new BaseLineTextRenderer(), XD_1, 17, 2.0, true, false, true));
        registry.register(linear("DATALOGIC2OF5", "Datalogic 2 of 5", new Datalogic2of5Encoder(), new WideRatioCodedRenderer(),
                new BaseLineTextRenderer(), XD_1, 17, 2.0, true, true, true));
        registry.register(linear("POSTNET", "PostNet", new PostNetEncoder(), new HeightCodedRenderer(),
                new BaseLineTextRenderer(), XD_2, 2, 2.0, true, false, false));

        registry.register(BarcodeRegistry.qrCode());
        registry.register(BarcodeRegistry.dataMatrix());
    }

    private static LinearBarcodeFormat linear(String id, String name,
                                              LinearEncoder encoder,
                                              LinearRenderer renderer,
                                              LinearTextRenderer textRenderer,
                                              double xDimension, double barHeight, double wideRatio,
                                              boolean checkDigit, boolean showCheckDigit, boolean showText) {
        FormatSpec spec = BarcodeRegistry.linear(id, name, checkDigit, showText,
                renderer instanceof WideRatioCodedRenderer);
        return new LinearBarcodeFormat(encoder, renderer, textRenderer, spec,
                xDimension, barHeight, wideRatio, checkDigit, showCheckDigit, showText);
    }
}
