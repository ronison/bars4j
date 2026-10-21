package org.bars4j.encode.matrix.aztec;

import java.nio.charset.Charset;
import java.util.Locale;

/**
 * Maps JDK character sets to the ECI assignment numbers used by the Aztec {@code FLG(n)} escape.
 *
 * <p>Only the character sets commonly used for barcodes are recognised. When no ECI number is
 * known the caller should encode without an ECI, assuming the default {@code ISO-8859-1} mapping.
 */
public final class AztecEci {

    private AztecEci() {
    }

    /**
     * Returns the ECI assignment number for {@code charset}.
     *
     * @throws IllegalArgumentException if the character set has no known ECI number
     */
    public static int valueFor(Charset charset) {
        String name = charset.name().toUpperCase(Locale.ROOT);
        switch (name) {
            case "ISO-8859-1":
            case "ISO8859-1":
            case "ISO88591":
                return 1;
            case "UTF-8":
            case "UTF8":
                return 26;
            case "US-ASCII":
            case "ASCII":
                return 27;
            case "UTF-16BE":
            case "UNICODEBIGUNMARKED":
                return 25;
            case "WINDOWS-1250":
            case "CP1250":
                return 21;
            case "WINDOWS-1251":
            case "CP1251":
                return 22;
            case "WINDOWS-1252":
            case "CP1252":
                return 23;
            case "WINDOWS-1256":
            case "CP1256":
                return 24;
            case "BIG5":
                return 28;
            case "GB18030":
            case "GB2312":
            case "GBK":
                return 29;
            case "EUC-KR":
            case "EUCKR":
                return 30;
            default:
                throw new IllegalArgumentException("No ECI code for character set " + charset);
        }
    }
}
