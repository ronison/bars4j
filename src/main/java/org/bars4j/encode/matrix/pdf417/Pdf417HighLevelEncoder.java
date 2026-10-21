package org.bars4j.encode.matrix.pdf417;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bars4j.encode.api.UnsupportedTextException;

/**
 * High-level encoder for PDF417, implementing the compaction schemes of Annex P of ISO/IEC 15438.
 *
 * <p>A message is turned into a stream of codewords (0-928). Text, Byte and Numeric compaction are
 * supported, alone or blended by {@link Pdf417Compaction#AUTO}. When the configured character set
 * is not the default, an Extended Channel Interpretation is announced before the first codeword.
 *
 * <p>The automatic scheme first splits the message into stretches that suit one compaction each,
 * then writes those stretches out. Splitting and writing are kept separate so that the
 * "which scheme fits" decision is independent of the codeword bookkeeping.
 */
public final class Pdf417HighLevelEncoder {

    /** Latch codeword selecting Text compaction. */
    private static final int TEXT_LATCH = 900;
    /** Latch codeword selecting Byte compaction for a byte count that is not a multiple of six. */
    private static final int BYTE_LATCH_PADDED = 901;
    /** Latch codeword selecting Numeric compaction. */
    private static final int NUMERIC_LATCH = 902;
    /** Shift codeword writing a single byte without leaving Text compaction. */
    private static final int BYTE_SHIFT = 913;
    /** Latch codeword selecting Byte compaction for a byte count that is a multiple of six. */
    private static final int BYTE_LATCH_ALIGNED = 924;
    /** Codeword introducing an Extended Channel Interpretation designator. */
    private static final int ECI_MARKER = 927;

    /** Codewords in a Text compaction group are base 30, with this value used as the odd pad. */
    private static final int TEXT_RADIX = 30;
    private static final int TEXT_PAD = 29;

    /** Largest number of digits combined into one Numeric compaction group. */
    private static final int NUMERIC_GROUP_DIGITS = 44;

    /** Number of bytes that share a base-900 Byte compaction group. */
    private static final int BYTE_GROUP_SIZE = 6;
    /** Codewords produced by one Byte compaction group. */
    private static final int BYTE_GROUP_CODEWORDS = 5;
    /** Radix shared by Numeric and Byte compaction groups. */
    private static final int GROUP_RADIX = 900;

    /** Characters reachable from the "Mixed" sub-mode, indexed by their compaction code. */
    private static final int[] MIXED_SUBMODE_CHARS = {
            48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 38, 13, 9, 44, 58,
            35, 45, 46, 36, 47, 43, 37, 42, 61, 94, 0, 32, 0, 0, 0};

    /** Characters reachable from the "Punctuation" sub-mode, indexed by their compaction code. */
    private static final int[] PUNCTUATION_SUBMODE_CHARS = {
            59, 60, 62, 64, 91, 92, 93, 95, 96, 126, 33, 13, 9, 44, 58,
            10, 45, 46, 36, 47, 34, 124, 42, 40, 41, 63, 123, 125, 39, 0};

    /** Reverse lookups from an ASCII character to its compaction code, or -1. */
    private static final int[] MIXED_CODE = new int[128];
    private static final int[] PUNCTUATION_CODE = new int[128];

    private static final Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;

    static {
        indexCodes(MIXED_SUBMODE_CHARS, MIXED_CODE);
        indexCodes(PUNCTUATION_SUBMODE_CHARS, PUNCTUATION_CODE);
    }

    private Pdf417HighLevelEncoder() {
    }

    /** The four Text compaction sub-modes. */
    private enum TextSubmode {
        UPPER, LOWER, MIXED, PUNCTUATION
    }

    /** The three compaction schemes, used as the state carried between stretches of a message. */
    private enum Scheme {
        TEXT, BYTE, NUMERIC
    }

    /** A stretch of the message that will be written with a single compaction scheme. */
    private record Stretch(Scheme scheme, int start, int length) {
    }

    /**
     * Encodes a message into PDF417 high-level codewords.
     *
     * @param msg        the message, must not be empty
     * @param compaction the compaction scheme to apply
     * @param encoding   character set used for Text/Byte compaction, {@code null} for ISO-8859-1
     * @return the codewords as chars in the range 0-928
     * @throws UnsupportedTextException if the message is empty or contains characters that cannot be
     *                                  represented with the selected character set
     */
    public static String encodeHighLevel(String msg, Pdf417Compaction compaction, Charset encoding)
            throws UnsupportedTextException {
        if (msg.isEmpty()) {
            throw new UnsupportedTextException("Empty message not allowed");
        }
        Charset charset = encoding != null ? encoding : DEFAULT_CHARSET;

        StringBuilder codewords = new StringBuilder(msg.length());
        if (!DEFAULT_CHARSET.equals(charset)) {
            Integer designator = designatorFor(charset);
            if (designator != null) {
                appendEci(designator, codewords);
            }
        }

        Pdf417Compaction mode = compaction != null ? compaction : Pdf417Compaction.AUTO;
        switch (mode) {
            case TEXT:
                appendText(msg, 0, msg.length(), codewords, TextSubmode.UPPER);
                break;
            case NUMERIC:
                codewords.append((char) NUMERIC_LATCH);
                appendNumeric(msg, 0, msg.length(), codewords);
                break;
            case BYTE:
                appendBytes(codewords, msg.getBytes(charset), Scheme.BYTE);
                break;
            default:
                appendAutomatic(msg, charset, codewords);
                break;
        }
        return codewords.toString();
    }

    /**
     * Splits the message into stretches and writes them with the scheme best suited to each.
     *
     * @param msg     the message
     * @param charset character set used for Byte compaction
     * @param out     receives the codewords
     * @throws UnsupportedTextException if a character cannot be represented
     */
    private static void appendAutomatic(String msg, Charset charset, StringBuilder out)
            throws UnsupportedTextException {
        List<Stretch> stretches = split(msg, charset);
        Scheme previous = Scheme.TEXT;
        TextSubmode submode = TextSubmode.UPPER;

        for (Stretch stretch : stretches) {
            switch (stretch.scheme()) {
                case NUMERIC:
                    out.append((char) NUMERIC_LATCH);
                    appendNumeric(msg, stretch.start(), stretch.length(), out);
                    previous = Scheme.NUMERIC;
                    submode = TextSubmode.UPPER;
                    break;
                case TEXT:
                    if (previous != Scheme.TEXT) {
                        out.append((char) TEXT_LATCH);
                        submode = TextSubmode.UPPER;
                    }
                    submode = appendText(msg, stretch.start(), stretch.length(), out, submode);
                    previous = Scheme.TEXT;
                    break;
                default: // BYTE
                    byte[] bytes = msg.substring(stretch.start(), stretch.start() + stretch.length())
                            .getBytes(charset);
                    if (bytes.length == 1 && previous == Scheme.TEXT) {
                        out.append((char) BYTE_SHIFT);
                        out.append((char) (bytes[0] & 0xFF));
                    } else {
                        appendBytes(out, bytes, previous);
                        previous = Scheme.BYTE;
                        submode = TextSubmode.UPPER;
                    }
                    break;
            }
        }
    }

    /**
     * Divides the message into stretches, greedily preferring Numeric, then Text, then Byte.
     *
     * @param msg     the message
     * @param charset character set used to test whether bytes are representable
     * @return the stretches in message order
     * @throws UnsupportedTextException if a character cannot be represented
     */
    private static List<Stretch> split(String msg, Charset charset) throws UnsupportedTextException {
        List<Stretch> stretches = new ArrayList<>();
        CharsetEncoder encoder = charset.newEncoder();
        int length = msg.length();
        int pos = 0;
        while (pos < length) {
            int digits = countLeadingDigits(msg, pos);
            if (digits >= 13) {
                stretches.add(new Stretch(Scheme.NUMERIC, pos, digits));
                pos += digits;
                continue;
            }
            int text = countTextRun(msg, pos);
            if (text >= 5 || digits == length) {
                stretches.add(new Stretch(Scheme.TEXT, pos, text));
                pos += text;
            } else {
                int bytes = countByteRun(msg, pos, encoder);
                if (bytes == 0) {
                    bytes = 1;
                }
                stretches.add(new Stretch(Scheme.BYTE, pos, bytes));
                pos += bytes;
            }
        }
        return stretches;
    }

    /**
     * Writes a run of characters using Text compaction.
     *
     * @return the sub-mode reached at the end, so a following text run can continue without
     *         re-latching
     */
    private static TextSubmode appendText(String msg, int start, int count, StringBuilder out,
                                          TextSubmode initial) {
        StringBuilder codes = new StringBuilder(count);
        TextSubmode submode = initial;
        int index = 0;
        while (true) {
            char ch = msg.charAt(start + index);
            switch (submode) {
                case UPPER:
                    if (isUpper(ch)) {
                        codes.append((char) (ch == ' ' ? 26 : ch - 'A'));
                    } else if (isLower(ch)) {
                        submode = TextSubmode.LOWER;
                        codes.append((char) 27);
                        continue;
                    } else if (isMixed(ch)) {
                        submode = TextSubmode.MIXED;
                        codes.append((char) 28);
                        continue;
                    } else {
                        codes.append((char) 29);
                        codes.append((char) PUNCTUATION_CODE[ch]);
                    }
                    break;
                case LOWER:
                    if (isLower(ch)) {
                        codes.append((char) (ch == ' ' ? 26 : ch - 'a'));
                    } else if (isUpper(ch)) {
                        codes.append((char) 27);
                        codes.append((char) (ch - 'A'));
                    } else if (isMixed(ch)) {
                        submode = TextSubmode.MIXED;
                        codes.append((char) 28);
                        continue;
                    } else {
                        codes.append((char) 29);
                        codes.append((char) PUNCTUATION_CODE[ch]);
                    }
                    break;
                case MIXED:
                    if (isMixed(ch)) {
                        codes.append((char) MIXED_CODE[ch]);
                    } else if (isUpper(ch)) {
                        submode = TextSubmode.UPPER;
                        codes.append((char) 28);
                        continue;
                    } else if (isLower(ch)) {
                        submode = TextSubmode.LOWER;
                        codes.append((char) 27);
                        continue;
                    } else if (start + index + 1 < count && isPunctuation(msg.charAt(start + index + 1))) {
                        submode = TextSubmode.PUNCTUATION;
                        codes.append((char) 25);
                        continue;
                    } else {
                        codes.append((char) 29);
                        codes.append((char) PUNCTUATION_CODE[ch]);
                    }
                    break;
                default: // PUNCTUATION
                    if (isPunctuation(ch)) {
                        codes.append((char) PUNCTUATION_CODE[ch]);
                    } else {
                        submode = TextSubmode.UPPER;
                        codes.append((char) 29);
                        continue;
                    }
                    break;
            }
            index++;
            if (index >= count) {
                break;
            }
        }
        packBase30(codes, out);
        return submode;
    }

    /** Packs base-30 text codes two at a time into codewords, padding an odd run with 29. */
    private static void packBase30(StringBuilder codes, StringBuilder out) {
        int count = codes.length();
        for (int i = 0; i + 1 < count; i += 2) {
            out.append((char) (codes.charAt(i) * TEXT_RADIX + codes.charAt(i + 1)));
        }
        if (count % 2 != 0) {
            out.append((char) (codes.charAt(count - 1) * TEXT_RADIX + TEXT_PAD));
        }
    }

    /** Writes a run of digits using Numeric compaction, in groups of at most 44 digits. */
    private static void appendNumeric(String msg, int start, int count, StringBuilder out) {
        BigInteger radix = BigInteger.valueOf(GROUP_RADIX);
        StringBuilder group = new StringBuilder();
        int index = 0;
        while (index < count) {
            int take = Math.min(NUMERIC_GROUP_DIGITS, count - index);
            BigInteger value = new BigInteger(
                    "1" + msg.substring(start + index, start + index + take));
            group.setLength(0);
            do {
                group.append((char) value.mod(radix).intValue());
                value = value.divide(radix);
            } while (value.signum() != 0);
            for (int i = group.length() - 1; i >= 0; i--) {
                out.append(group.charAt(i));
            }
            index += take;
        }
    }

    /**
     * Writes bytes using Byte compaction.
     *
     * @param previous the scheme in effect beforehand; a lone byte may be shifted from Text
     *                 compaction instead of latching to Byte compaction
     */
    private static void appendBytes(StringBuilder out, byte[] bytes, Scheme previous) {
        if (bytes.length == 1 && previous == Scheme.TEXT) {
            out.append((char) BYTE_SHIFT);
        } else if (bytes.length % BYTE_GROUP_SIZE == 0) {
            out.append((char) BYTE_LATCH_ALIGNED);
        } else {
            out.append((char) BYTE_LATCH_PADDED);
        }

        int index = 0;
        while (bytes.length - index >= BYTE_GROUP_SIZE) {
            long packed = 0;
            for (int i = 0; i < BYTE_GROUP_SIZE; i++) {
                packed = (packed << 8) | (bytes[index + i] & 0xFF);
            }
            int[] group = new int[BYTE_GROUP_CODEWORDS];
            for (int i = 0; i < BYTE_GROUP_CODEWORDS; i++) {
                group[i] = (int) (packed % GROUP_RADIX);
                packed /= GROUP_RADIX;
            }
            for (int i = BYTE_GROUP_CODEWORDS - 1; i >= 0; i--) {
                out.append((char) group[i]);
            }
            index += BYTE_GROUP_SIZE;
        }
        for (; index < bytes.length; index++) {
            out.append((char) (bytes[index] & 0xFF));
        }
    }

    /** Writes the ECI designator sequence for a character set. */
    private static void appendEci(int designator, StringBuilder out) {
        if (designator >= 0 && designator < GROUP_RADIX) {
            out.append((char) ECI_MARKER);
            out.append((char) designator);
        } else {
            throw new IllegalArgumentException("ECI designator out of range: " + designator);
        }
    }

    /** Maps a character set to its ECI designator, or {@code null} when it has none here. */
    private static Integer designatorFor(Charset charset) {
        if (charset.equals(StandardCharsets.UTF_8)) {
            return 26;
        }
        if (charset.equals(StandardCharsets.US_ASCII)) {
            return 27;
        }
        if (charset.equals(StandardCharsets.UTF_16BE)) {
            return 25;
        }
        String name = charset.name();
        for (int part = 1; part <= 16; part++) {
            if (part == 12 || part == 14) {
                continue; // no such ISO-8859 part exists
            }
            if (name.equals("ISO-8859-" + part)) {
                return part == 1 ? 1 : part + 2; // ECI 1 is ISO-8859-1, then ECI 4 onward
            }
        }
        if (name.equals("windows-1250")) {
            return 21;
        }
        if (name.equals("windows-1251")) {
            return 22;
        }
        if (name.equals("windows-1252")) {
            return 23;
        }
        if (name.equals("windows-1256")) {
            return 24;
        }
        return null;
    }

    /** Fills a reverse lookup table from compaction codes to ASCII characters. */
    private static void indexCodes(int[] characters, int[] codes) {
        Arrays.fill(codes, -1);
        for (int code = 0; code < characters.length; code++) {
            if (characters[code] > 0) {
                codes[characters[code]] = code;
            }
        }
    }

    private static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    private static boolean isUpper(char ch) {
        return ch == ' ' || (ch >= 'A' && ch <= 'Z');
    }

    private static boolean isLower(char ch) {
        return ch == ' ' || (ch >= 'a' && ch <= 'z');
    }

    private static boolean isMixed(char ch) {
        return ch < MIXED_CODE.length && MIXED_CODE[ch] != -1;
    }

    private static boolean isPunctuation(char ch) {
        return ch < PUNCTUATION_CODE.length && PUNCTUATION_CODE[ch] != -1;
    }

    private static boolean isText(char ch) {
        return ch == '\t' || ch == '\n' || ch == '\r' || (ch >= 32 && ch <= 126);
    }

    /** Number of consecutive digits starting at a position. */
    private static int countLeadingDigits(String msg, int start) {
        int count = 0;
        while (start + count < msg.length() && isDigit(msg.charAt(start + count))) {
            count++;
        }
        return count;
    }

    /**
     * Length of the run that Text compaction can cover starting at a position.
     *
     * <p>Digits count as text in short runs, but a run of thirteen or more digits belongs to
     * Numeric compaction, so the run ends before it.
     */
    private static int countTextRun(String msg, int start) {
        int length = msg.length();
        int index = start;
        while (index < length) {
            int digits = 0;
            while (digits < 13 && index < length && isDigit(msg.charAt(index))) {
                digits++;
                index++;
            }
            if (digits >= 13) {
                return index - start - digits;
            }
            if (digits > 0) {
                continue;
            }
            if (!isText(msg.charAt(index))) {
                break;
            }
            index++;
        }
        return index - start;
    }

    /**
     * Length of the run that Byte compaction should cover starting at a position.
     *
     * <p>The run ends before a Numeric run, and rejects characters the character set cannot
     * represent.
     *
     * @throws UnsupportedTextException if a character cannot be represented
     */
    private static int countByteRun(String msg, int start, CharsetEncoder encoder)
            throws UnsupportedTextException {
        int length = msg.length();
        int index = start;
        while (index < length) {
            int digits = 0;
            int scan = index;
            while (digits < 13 && scan < length && isDigit(msg.charAt(scan))) {
                digits++;
                scan++;
            }
            if (digits >= 13) {
                return index - start;
            }
            if (!encoder.canEncode(msg.charAt(index))) {
                char ch = msg.charAt(index);
                throw new UnsupportedTextException("Non-encodable character detected: " + ch
                        + " (Unicode: " + (int) ch + ")");
            }
            index++;
        }
        return index - start;
    }
}
