package org.bars4j.encode.matrix.qr;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Encodes input text into {@link QrSegment}s, choosing the most compact single mode that fits.
 */
public final class QrSegmentEncoder {

    /** QRCode alphanumeric character set, indexed by character value. */
    public static final String ALPHANUMERIC_CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:";

    /** Charset used by Kanji mode (also known as Shift-JIS). */
    private static final Charset KANJI_CHARSET = Charset.forName("Shift_JIS");

    private QrSegmentEncoder() {
    }

    public static QrSegment encode(String text, Charset charset) {
        if (isNumeric(text)) {
            return numeric(text);
        }
        if (isKanji(text)) {
            return kanji(text);
        }
        if (isAlphanumeric(text)) {
            return alphanumeric(text);
        }
        return bytes(text, charset);
    }

    private static boolean isNumeric(String text) {
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

    private static boolean isAlphanumeric(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (ALPHANUMERIC_CHARSET.indexOf(text.charAt(i)) < 0) {
                return false;
            }
        }
        return !text.isEmpty();
    }

    /** True when every character is a two-byte Shift-JIS character inside the Kanji ranges. */
    private static boolean isKanji(String text) {
        if (text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            int code = shiftJisCode(text.charAt(i));
            if (!isKanjiCode(code)) {
                return false;
            }
        }
        return true;
    }

    private static int shiftJisCode(char c) {
        byte[] bytes = String.valueOf(c).getBytes(KANJI_CHARSET);
        if (bytes.length != 2) {
            return -1;
        }
        return ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF);
    }

    private static boolean isKanjiCode(int code) {
        return (code >= 0x8140 && code <= 0x9FFC) || (code >= 0xE040 && code <= 0xEBBF);
    }

    private static QrSegment numeric(String text) {
        List<Integer> values = new ArrayList<>();
        List<Integer> widths = new ArrayList<>();
        for (int i = 0; i < text.length(); i += 3) {
            int length = Math.min(3, text.length() - i);
            int value = Integer.parseInt(text.substring(i, i + length));
            values.add(value);
            widths.add(length == 3 ? 10 : length == 2 ? 7 : 4);
        }
        return new QrSegment(QrMode.NUMERIC, toArray(values), toArray(widths), text.length());
    }

    private static QrSegment alphanumeric(String text) {
        List<Integer> values = new ArrayList<>();
        List<Integer> widths = new ArrayList<>();
        int i = 0;
        for (; i + 1 < text.length(); i += 2) {
            values.add(ALPHANUMERIC_CHARSET.indexOf(text.charAt(i)) * 45
                    + ALPHANUMERIC_CHARSET.indexOf(text.charAt(i + 1)));
            widths.add(11);
        }
        if (i < text.length()) {
            values.add(ALPHANUMERIC_CHARSET.indexOf(text.charAt(i)));
            widths.add(6);
        }
        return new QrSegment(QrMode.ALPHANUMERIC, toArray(values), toArray(widths), text.length());
    }

    private static QrSegment kanji(String text) {
        List<Integer> values = new ArrayList<>();
        List<Integer> widths = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            int code = shiftJisCode(text.charAt(i));
            int subtracted = (code >= 0x8140 && code <= 0x9FFC) ? code - 0x8140 : code - 0xC140;
            int value = ((subtracted >>> 8) * 0xC0) + (subtracted & 0xFF);
            values.add(value);
            widths.add(13);
        }
        return new QrSegment(QrMode.KANJI, toArray(values), toArray(widths), text.length());
    }

    private static QrSegment bytes(String text, Charset charset) {
        byte[] encoded = text.getBytes(charset);
        int[] values = new int[encoded.length];
        int[] widths = new int[encoded.length];
        for (int i = 0; i < encoded.length; i++) {
            values[i] = encoded[i] & 0xFF;
            widths[i] = 8;
        }
        return new QrSegment(QrMode.BYTE, values, widths, encoded.length);
    }

    private static int[] toArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    /** Default charset used for byte mode when none is provided (matches ZXing's default). */
    public static Charset defaultCharset() {
        return StandardCharsets.ISO_8859_1;
    }
}
