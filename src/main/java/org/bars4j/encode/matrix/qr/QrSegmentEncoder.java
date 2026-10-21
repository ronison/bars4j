package org.bars4j.encode.matrix.qr;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Turns input text into a {@link QrSegment}, choosing the densest single data mode able to
 * represent the whole string.
 */
public final class QrSegmentEncoder {

    /** Characters of alphanumeric mode, listed in their 6-bit code order (ISO/IEC 18004). */
    public static final String ALPHANUMERIC_CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:";

    /** Two-byte encoding used for Kanji mode. */
    private static final Charset SHIFT_JIS = Charset.forName("Shift_JIS");

    /** Lower and upper Shift-JIS code ranges that Kanji mode can represent. */
    private static final int KANJI_SJIS_FIRST = 0x8140;
    private static final int KANJI_SJIS_FIRST_LAST = 0x9FFC;
    private static final int KANJI_SJIS_SECOND = 0xE040;
    private static final int KANJI_SJIS_SECOND_LAST = 0xEBBF;

    /** Reverse lookup for {@link #ALPHANUMERIC_CHARSET}; negative where the character is absent. */
    private static final int[] ALPHANUMERIC_CODE = new int[128];

    static {
        for (int i = 0; i < ALPHANUMERIC_CODE.length; i++) {
            ALPHANUMERIC_CODE[i] = -1;
        }
        for (int i = 0; i < ALPHANUMERIC_CHARSET.length(); i++) {
            ALPHANUMERIC_CODE[ALPHANUMERIC_CHARSET.charAt(i)] = i;
        }
    }

    private QrSegmentEncoder() {
    }

    public static QrSegment encode(String text, Charset charset) {
        if (isAllDigits(text)) {
            return numericSegment(text);
        }
        if (isAllKanji(text)) {
            return kanjiSegment(text);
        }
        if (isAllAlphanumeric(text)) {
            return alphanumericSegment(text);
        }
        return byteSegment(text, charset);
    }

    /** Charset used by byte mode when the caller does not choose one. */
    public static Charset defaultCharset() {
        return StandardCharsets.ISO_8859_1;
    }

    private static boolean isAllDigits(String text) {
        if (text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAllAlphanumeric(String text) {
        if (text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (alphanumericCode(text.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }

    /** True when every character is a two-byte Shift-JIS value inside the Kanji ranges. */
    private static boolean isAllKanji(String text) {
        if (text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!isKanjiValue(shiftJisValue(text.charAt(i)))) {
                return false;
            }
        }
        return true;
    }

    private static int alphanumericCode(char c) {
        return c < ALPHANUMERIC_CODE.length ? ALPHANUMERIC_CODE[c] : -1;
    }

    private static int shiftJisValue(char c) {
        byte[] encoded = String.valueOf(c).getBytes(SHIFT_JIS);
        if (encoded.length != 2) {
            return -1;
        }
        return ((encoded[0] & 0xFF) << 8) | (encoded[1] & 0xFF);
    }

    private static boolean isKanjiValue(int code) {
        return code >= KANJI_SJIS_FIRST && code <= KANJI_SJIS_FIRST_LAST
                || code >= KANJI_SJIS_SECOND && code <= KANJI_SJIS_SECOND_LAST;
    }

    private static QrSegment numericSegment(String text) {
        int length = text.length();
        int groups = (length + 2) / 3;
        int[] values = new int[groups];
        int[] widths = new int[groups];
        for (int g = 0; g < groups; g++) {
            int from = g * 3;
            int to = Math.min(from + 3, length);
            int packed = 0;
            for (int i = from; i < to; i++) {
                packed = packed * 10 + Character.digit(text.charAt(i), 10);
            }
            int digits = to - from;
            values[g] = packed;
            widths[g] = digits == 1 ? 4 : digits == 2 ? 7 : 10;
        }
        return new QrSegment(QrMode.NUMERIC, values, widths, length);
    }

    private static QrSegment alphanumericSegment(String text) {
        int length = text.length();
        int pairs = length / 2;
        int extra = length % 2;
        int[] values = new int[pairs + extra];
        int[] widths = new int[pairs + extra];
        int index = 0;
        for (int p = 0; p < pairs; p++) {
            values[index] = alphanumericCode(text.charAt(2 * p)) * 45
                    + alphanumericCode(text.charAt(2 * p + 1));
            widths[index++] = 11;
        }
        if (extra == 1) {
            values[index] = alphanumericCode(text.charAt(length - 1));
            widths[index] = 6;
        }
        return new QrSegment(QrMode.ALPHANUMERIC, values, widths, length);
    }

    private static QrSegment kanjiSegment(String text) {
        int length = text.length();
        int[] values = new int[length];
        int[] widths = new int[length];
        for (int i = 0; i < length; i++) {
            int code = shiftJisValue(text.charAt(i));
            int base = code <= KANJI_SJIS_FIRST_LAST ? KANJI_SJIS_FIRST : 0xC140;
            int adjusted = code - base;
            values[i] = (adjusted >>> 8) * 0xC0 + (adjusted & 0xFF);
            widths[i] = 13;
        }
        return new QrSegment(QrMode.KANJI, values, widths, length);
    }

    private static QrSegment byteSegment(String text, Charset charset) {
        byte[] encoded = text.getBytes(charset);
        int[] values = new int[encoded.length];
        int[] widths = new int[encoded.length];
        for (int i = 0; i < encoded.length; i++) {
            values[i] = encoded[i] & 0xFF;
            widths[i] = 8;
        }
        return new QrSegment(QrMode.BYTE, values, widths, encoded.length);
    }
}
