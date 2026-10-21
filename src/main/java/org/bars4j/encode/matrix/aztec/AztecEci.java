package org.bars4j.encode.matrix.aztec;

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;

/**
 * Maps JDK character sets to the ECI assignment numbers used by the Aztec {@code FLG(n)} escape.
 *
 * <p>Only the character sets commonly seen in barcodes are listed. When a character set has no
 * assignment number the caller should encode without an ECI and fall back to the default Latin-1
 * interpretation.
 */
public final class AztecEci {

    private static final Map<String, Integer> ASSIGNMENTS = Map.ofEntries(
            Map.entry("ISO-8859-1", 1),
            Map.entry("ISO8859-1", 1),
            Map.entry("ISO88591", 1),
            Map.entry("UTF-8", 26),
            Map.entry("UTF8", 26),
            Map.entry("US-ASCII", 27),
            Map.entry("ASCII", 27),
            Map.entry("UTF-16BE", 25),
            Map.entry("UNICODEBIGUNMARKED", 25),
            Map.entry("WINDOWS-1250", 21),
            Map.entry("CP1250", 21),
            Map.entry("WINDOWS-1251", 22),
            Map.entry("CP1251", 22),
            Map.entry("WINDOWS-1252", 23),
            Map.entry("CP1252", 23),
            Map.entry("WINDOWS-1256", 24),
            Map.entry("CP1256", 24),
            Map.entry("BIG5", 28),
            Map.entry("GB18030", 29),
            Map.entry("GB2312", 29),
            Map.entry("GBK", 29),
            Map.entry("EUC-KR", 30),
            Map.entry("EUCKR", 30));

    private AztecEci() {
    }

    /**
     * Returns the ECI assignment number for {@code charset}.
     *
     * @throws IllegalArgumentException if the character set has no known ECI number
     */
    public static int valueFor(Charset charset) {
        Integer assignment = ASSIGNMENTS.get(charset.name().toUpperCase(Locale.ROOT));
        if (assignment == null) {
            throw new IllegalArgumentException("No ECI code for character set " + charset);
        }
        return assignment;
    }
}
