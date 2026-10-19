package org.bars4j.format;

import org.bars4j.BarcodeFactory;

/**
 * Registers the formats shipped with the library.
 */
final class BuiltinFormats {

    private BuiltinFormats() {
    }

    static void register(BarcodeRegistry registry) {
        BarcodeFactory f = BarcodeRegistry.factory();

        registry.register(new LinearBarcodeFormat(f::createEAN13, BarcodeRegistry.linear("EAN13", "EAN-13", true, true, false)));
        registry.register(new LinearBarcodeFormat(f::createEAN8, BarcodeRegistry.linear("EAN8", "EAN-8", true, true, false)));
        registry.register(new LinearBarcodeFormat(f::createEAN5, BarcodeRegistry.linear("EAN5", "EAN-5", false, true, false)));
        registry.register(new LinearBarcodeFormat(f::createUPCA, BarcodeRegistry.linear("UPCA", "UPC-A", true, true, false)));
        registry.register(new LinearBarcodeFormat(f::createUPCE, BarcodeRegistry.linear("UPCE", "UPC-E", true, true, false)));
        registry.register(new LinearBarcodeFormat(f::createCodabar, BarcodeRegistry.linear("CODABAR", "Codabar", false, true, true)));
        registry.register(new LinearBarcodeFormat(f::createCode39, BarcodeRegistry.linear("CODE39", "Code 39", false, true, true)));
        registry.register(new LinearBarcodeFormat(f::createCode39Extended, BarcodeRegistry.linear("CODE39EXT", "Code 39 Extended", false, true, true)));
        registry.register(new LinearBarcodeFormat(f::createCode93, BarcodeRegistry.linear("CODE93", "Code 93", true, true, false)));
        registry.register(new LinearBarcodeFormat(f::createCode93Extended, BarcodeRegistry.linear("CODE93EXT", "Code 93 Extended", true, true, false)));
        registry.register(new LinearBarcodeFormat(f::createCode128, BarcodeRegistry.linear("CODE128", "Code 128", true, true, false)));
        registry.register(new LinearBarcodeFormat(f::createCode11, BarcodeRegistry.linear("CODE11", "Code 11", true, true, false)));
        registry.register(new LinearBarcodeFormat(f::createMSIPlessey, BarcodeRegistry.linear("MSIPLESSEY", "MSI Plessey", true, true, false)));
        registry.register(new LinearBarcodeFormat(f::createStandard2of5, BarcodeRegistry.linear("STANDARD2OF5", "Standard 2 of 5", true, true, true)));
        registry.register(new LinearBarcodeFormat(f::createIATA2of5, BarcodeRegistry.linear("IATA2OF5", "IATA 2 of 5", true, true, true)));
        registry.register(new LinearBarcodeFormat(f::createInterleaved2of5, BarcodeRegistry.linear("INTERLEAVED2OF5", "Interleaved 2 of 5", true, true, true)));
        registry.register(new LinearBarcodeFormat(f::createMatrix2of5, BarcodeRegistry.linear("MATRIX2OF5", "Matrix 2 of 5", true, true, true)));
        registry.register(new LinearBarcodeFormat(f::createDatalogic2of5, BarcodeRegistry.linear("DATALOGIC2OF5", "Datalogic 2 of 5", true, true, true)));
        registry.register(new LinearBarcodeFormat(f::createPostNet, BarcodeRegistry.linear("POSTNET", "PostNet", true, false, false)));

        registry.register(BarcodeRegistry.qrCode());
    }
}
