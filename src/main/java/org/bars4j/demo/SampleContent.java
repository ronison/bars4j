package org.bars4j.demo;

import java.util.Map;

/**
 * Default sample content for each registered format, used by the desktop demo.
 *
 * <p>Every sample is chosen so that it encodes successfully with its format's default options.
 */
public final class SampleContent {

    private static final Map<String, String> SAMPLES = Map.ofEntries(
            Map.entry("EAN13", "789100031550"),
            Map.entry("EAN8", "9788515"),
            Map.entry("EAN5", "54495"),
            Map.entry("UPCA", "07567816415"),
            Map.entry("UPCE", "0756785"),
            Map.entry("CODABAR", "97885150040-85"),
            Map.entry("CODE39", "BARS4J-39"),
            Map.entry("CODE39EXT", "Bars4J-39Ext"),
            Map.entry("CODE93", "BARS4J-93"),
            Map.entry("CODE93EXT", "Bars4J-93Ext"),
            Map.entry("CODE128", "Bars4J-128"),
            Map.entry("CODE11", "9788515004041"),
            Map.entry("MSIPLESSEY", "9788515004041"),
            Map.entry("STANDARD2OF5", "978851500404"),
            Map.entry("IATA2OF5", "978851500404"),
            Map.entry("MATRIX2OF5", "123456"),
            Map.entry("DATALOGIC2OF5", "123456"),
            Map.entry("INTERLEAVED2OF5", "9876543210921"),
            Map.entry("POSTNET", "805365961"),
            Map.entry("QRCODE", "https://github.com/ronison/bars4j"),
            Map.entry("DATAMATRIX", "https://github.com/ronison/bars4j"));

    private SampleContent() {
    }

    /** Returns a sample that encodes with the given format id, or {@code "Bars4J"} as fallback. */
    public static String forFormat(String formatId) {
        return SAMPLES.getOrDefault(formatId.toUpperCase(), "Bars4J");
    }
}
