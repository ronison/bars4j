package org.bars4j.encode.matrix.pdf417;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.bars4j.encode.api.UnsupportedTextException;

/**
 * PDF417 high-level encoder, following the encoding rules of Annex P of ISO/IEC 15438.
 *
 * <p>This turns a text message into a sequence of codewords (0-928) using Text, Byte and Numeric
 * compaction. Extended Channel Interpretations are announced with an ECI codeword when a non-default
 * character set is used. There is no dependency on any external library.
 */
public final class Pdf417HighLevelEncoder {

    /** Codeword latching to Text compaction. */
    private static final int LATCH_TO_TEXT = 900;
    /** Codeword latching to Byte compaction when the byte count is not a multiple of six. */
    private static final int LATCH_TO_BYTE_PADDED = 901;
    /** Codeword latching to Numeric compaction. */
    private static final int LATCH_TO_NUMERIC = 902;
    /** Codeword shifting a single byte in the middle of Text compaction. */
    private static final int SHIFT_TO_BYTE = 913;
    /** Codeword latching to Byte compaction when the byte count is a multiple of six. */
    private static final int LATCH_TO_BYTE = 924;
    /** Codeword introducing an ECI, followed by the ECI designator value. */
    private static final int ECI_CHARSET = 927;

    /** Internal marker for Text compaction. */
    private static final int TEXT_COMPACTION = 0;
    /** Internal marker for Byte compaction. */
    private static final int BYTE_COMPACTION = 1;
    /** Internal marker for Numeric compaction. */
    private static final int NUMERIC_COMPACTION = 2;

    private static final int SUBMODE_ALPHA = 0;
    private static final int SUBMODE_LOWER = 1;
    private static final int SUBMODE_MIXED = 2;
    private static final int SUBMODE_PUNCTUATION = 3;

    /** Text compaction codes for the "Mixed" sub-mode, indexed by code. */
    private static final int[] TEXT_MIXED_RAW = {
            48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 38, 13, 9, 44, 58,
            35, 45, 46, 36, 47, 43, 37, 42, 61, 94, 0, 32, 0, 0, 0};

    /** Text compaction codes for the "Punctuation" sub-mode, indexed by code. */
    private static final int[] TEXT_PUNCTUATION_RAW = {
            59, 60, 62, 64, 91, 92, 93, 95, 96, 126, 33, 13, 9, 44, 58,
            10, 45, 46, 36, 47, 34, 124, 42, 40, 41, 63, 123, 125, 39, 0};

    private static final int[] MIXED = new int[128];
    private static final int[] PUNCTUATION = new int[128];

    private static final Charset DEFAULT_ENCODING = StandardCharsets.ISO_8859_1;

    static {
        Arrays.fill(MIXED, -1);
        for (int i = 0; i < TEXT_MIXED_RAW.length; i++) {
            int ch = TEXT_MIXED_RAW[i];
            if (ch > 0) {
                MIXED[ch] = i;
            }
        }
        Arrays.fill(PUNCTUATION, -1);
        for (int i = 0; i < TEXT_PUNCTUATION_RAW.length; i++) {
            int ch = TEXT_PUNCTUATION_RAW[i];
            if (ch > 0) {
                PUNCTUATION[ch] = i;
            }
        }
    }

    private Pdf417HighLevelEncoder() {
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
        Charset charset = encoding != null ? encoding : DEFAULT_ENCODING;

        StringBuilder sb = new StringBuilder(msg.length());
        if (!DEFAULT_ENCODING.equals(charset)) {
            Integer eci = eciValue(charset);
            if (eci != null) {
                appendEci(eci, sb);
            }
        }

        int len = msg.length();
        int p = 0;
        int textSubMode = SUBMODE_ALPHA;
        Pdf417Compaction mode = compaction != null ? compaction : Pdf417Compaction.AUTO;

        switch (mode) {
            case TEXT:
                encodeText(msg, p, len, sb, textSubMode);
                break;
            case BYTE: {
                byte[] bytes = msg.getBytes(charset);
                encodeBinary(bytes, 0, bytes.length, BYTE_COMPACTION, sb);
                break;
            }
            case NUMERIC:
                sb.append((char) LATCH_TO_NUMERIC);
                encodeNumeric(msg, p, len, sb);
                break;
            default: {
                int encodingMode = TEXT_COMPACTION;
                CharsetEncoder encoder = charset.newEncoder();
                while (p < len) {
                    int n = determineConsecutiveDigitCount(msg, p);
                    if (n >= 13) {
                        sb.append((char) LATCH_TO_NUMERIC);
                        encodingMode = NUMERIC_COMPACTION;
                        textSubMode = SUBMODE_ALPHA;
                        encodeNumeric(msg, p, n, sb);
                        p += n;
                    } else {
                        int t = determineConsecutiveTextCount(msg, p);
                        if (t >= 5 || n == len) {
                            if (encodingMode != TEXT_COMPACTION) {
                                sb.append((char) LATCH_TO_TEXT);
                                encodingMode = TEXT_COMPACTION;
                                textSubMode = SUBMODE_ALPHA;
                            }
                            textSubMode = encodeText(msg, p, t, sb, textSubMode);
                            p += t;
                        } else {
                            int b = determineConsecutiveBinaryCount(msg, p, encoder);
                            if (b == 0) {
                                b = 1;
                            }
                            byte[] bytes = msg.substring(p, p + b).getBytes(charset);
                            if (bytes.length == 1 && encodingMode == TEXT_COMPACTION) {
                                encodeBinary(bytes, 0, 1, TEXT_COMPACTION, sb);
                            } else {
                                encodeBinary(bytes, 0, bytes.length, encodingMode, sb);
                                encodingMode = BYTE_COMPACTION;
                                textSubMode = SUBMODE_ALPHA;
                            }
                            p += b;
                        }
                    }
                }
                break;
            }
        }
        return sb.toString();
    }

    /**
     * Encodes a stretch of text using Text compaction.
     *
     * @return the sub-mode in which the method finished, so that a following call can continue
     *         without re-latching
     */
    private static int encodeText(String msg, int startpos, int count, StringBuilder sb, int initialSubmode) {
        StringBuilder tmp = new StringBuilder(count);
        int submode = initialSubmode;
        int idx = 0;
        while (true) {
            char ch = msg.charAt(startpos + idx);
            switch (submode) {
                case SUBMODE_ALPHA:
                    if (isAlphaUpper(ch)) {
                        tmp.append((char) (ch == ' ' ? 26 : ch - 65));
                    } else if (isAlphaLower(ch)) {
                        submode = SUBMODE_LOWER;
                        tmp.append((char) 27);
                        continue;
                    } else if (isMixed(ch)) {
                        submode = SUBMODE_MIXED;
                        tmp.append((char) 28);
                        continue;
                    } else {
                        tmp.append((char) 29);
                        tmp.append((char) PUNCTUATION[ch]);
                    }
                    break;
                case SUBMODE_LOWER:
                    if (isAlphaLower(ch)) {
                        tmp.append((char) (ch == ' ' ? 26 : ch - 97));
                    } else if (isAlphaUpper(ch)) {
                        tmp.append((char) 27);
                        tmp.append((char) (ch - 65));
                    } else if (isMixed(ch)) {
                        submode = SUBMODE_MIXED;
                        tmp.append((char) 28);
                        continue;
                    } else {
                        tmp.append((char) 29);
                        tmp.append((char) PUNCTUATION[ch]);
                    }
                    break;
                case SUBMODE_MIXED:
                    if (isMixed(ch)) {
                        tmp.append((char) MIXED[ch]);
                    } else if (isAlphaUpper(ch)) {
                        submode = SUBMODE_ALPHA;
                        tmp.append((char) 28);
                        continue;
                    } else if (isAlphaLower(ch)) {
                        submode = SUBMODE_LOWER;
                        tmp.append((char) 27);
                        continue;
                    } else if (startpos + idx + 1 < count && isPunctuation(msg.charAt(startpos + idx + 1))) {
                        submode = SUBMODE_PUNCTUATION;
                        tmp.append((char) 25);
                        continue;
                    } else {
                        tmp.append((char) 29);
                        tmp.append((char) PUNCTUATION[ch]);
                    }
                    break;
                default: // SUBMODE_PUNCTUATION
                    if (isPunctuation(ch)) {
                        tmp.append((char) PUNCTUATION[ch]);
                    } else {
                        submode = SUBMODE_ALPHA;
                        tmp.append((char) 29);
                        continue;
                    }
                    break;
            }
            idx++;
            if (idx >= count) {
                break;
            }
        }
        int len = tmp.length();
        char high = 0;
        for (int i = 0; i < len; i++) {
            if (i % 2 != 0) {
                high = (char) (high * 30 + tmp.charAt(i));
                sb.append(high);
            } else {
                high = tmp.charAt(i);
            }
        }
        if (len % 2 != 0) {
            sb.append((char) (high * 30 + 29));
        }
        return submode;
    }

    /**
     * Encodes bytes using Byte compaction.
     *
     * @param startmode the compaction mode in effect before this call; a lone byte may be shifted
     *                  from Text compaction instead of latching
     */
    private static void encodeBinary(byte[] bytes, int startpos, int count, int startmode, StringBuilder sb) {
        if (count == 1 && startmode == TEXT_COMPACTION) {
            sb.append((char) SHIFT_TO_BYTE);
        } else if (count % 6 == 0) {
            sb.append((char) LATCH_TO_BYTE);
        } else {
            sb.append((char) LATCH_TO_BYTE_PADDED);
        }

        int idx = startpos;
        if (count >= 6) {
            while (startpos + count - idx >= 6) {
                long value = 0;
                for (int i = 0; i < 6; i++) {
                    value <<= 8;
                    value += bytes[idx + i] & 0xFF;
                }
                int[] groups = new int[5];
                for (int i = 0; i < 5; i++) {
                    groups[i] = (int) (value % 900);
                    value /= 900;
                }
                for (int i = groups.length - 1; i >= 0; i--) {
                    sb.append((char) groups[i]);
                }
                idx += 6;
            }
        }
        for (int i = idx; i < startpos + count; i++) {
            sb.append((char) (bytes[i] & 0xFF));
        }
    }

    /** Encodes a run of digits using Numeric compaction (base 900 groups). */
    private static void encodeNumeric(String msg, int startpos, int count, StringBuilder sb) {
        int idx = 0;
        StringBuilder tmp = new StringBuilder(count / 3 + 1);
        BigInteger num900 = BigInteger.valueOf(900);
        BigInteger num0 = BigInteger.valueOf(0);
        while (idx < count) {
            tmp.setLength(0);
            int len = Math.min(44, count - idx);
            String part = "1" + msg.substring(startpos + idx, startpos + idx + len);
            BigInteger bigint = new BigInteger(part);
            do {
                tmp.append((char) bigint.mod(num900).intValue());
                bigint = bigint.divide(num900);
            } while (!bigint.equals(num0));
            for (int i = tmp.length() - 1; i >= 0; i--) {
                sb.append(tmp.charAt(i));
            }
            idx += len;
        }
    }

    private static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    private static boolean isAlphaUpper(char ch) {
        return ch == ' ' || (ch >= 'A' && ch <= 'Z');
    }

    private static boolean isAlphaLower(char ch) {
        return ch == ' ' || (ch >= 'a' && ch <= 'z');
    }

    private static boolean isMixed(char ch) {
        return ch < 128 && MIXED[ch] != -1;
    }

    private static boolean isPunctuation(char ch) {
        return ch < 128 && PUNCTUATION[ch] != -1;
    }

    private static boolean isText(char ch) {
        return ch == '\t' || ch == '\n' || ch == '\r' || (ch >= 32 && ch <= 126);
    }

    private static int determineConsecutiveDigitCount(String msg, int startpos) {
        int count = 0;
        int len = msg.length();
        int idx = startpos;
        while (idx < len && isDigit(msg.charAt(idx))) {
            count++;
            idx++;
        }
        return count;
    }

    private static int determineConsecutiveTextCount(String msg, int startpos) {
        int len = msg.length();
        int idx = startpos;
        while (idx < len) {
            int numericCount = 0;
            while (numericCount < 13 && idx < len && isDigit(msg.charAt(idx))) {
                numericCount++;
                idx++;
            }
            if (numericCount >= 13) {
                return idx - startpos - numericCount;
            }
            if (numericCount > 0) {
                continue;
            }
            if (!isText(msg.charAt(idx))) {
                break;
            }
            idx++;
        }
        return idx - startpos;
    }

    private static int determineConsecutiveBinaryCount(String msg, int startpos, CharsetEncoder encoder)
            throws UnsupportedTextException {
        int len = msg.length();
        int idx = startpos;
        while (idx < len) {
            int numericCount = 0;
            int i = idx;
            while (numericCount < 13 && i < len && isDigit(msg.charAt(i))) {
                numericCount++;
                i = idx + numericCount;
                if (i >= len) {
                    break;
                }
            }
            if (numericCount >= 13) {
                return idx - startpos;
            }
            if (!encoder.canEncode(msg.charAt(idx))) {
                char ch = msg.charAt(idx);
                throw new UnsupportedTextException("Non-encodable character detected: " + ch
                        + " (Unicode: " + (int) ch + ")");
            }
            idx++;
        }
        return idx - startpos;
    }

    /** Writes an ECI codeword sequence for the given designator value. */
    private static void appendEci(int eci, StringBuilder sb) {
        if (eci >= 0 && eci < 900) {
            sb.append((char) ECI_CHARSET);
            sb.append((char) eci);
        } else {
            throw new IllegalArgumentException("ECI designator out of range: " + eci);
        }
    }

    /** Returns the ECI designator for a few well-known character sets, or {@code null}. */
    private static Integer eciValue(Charset charset) {
        String name = charset.name();
        if (charset.equals(StandardCharsets.UTF_8)) {
            return 26;
        }
        if (charset.equals(StandardCharsets.US_ASCII)) {
            return 27;
        }
        if (charset.equals(StandardCharsets.UTF_16BE)) {
            return 25;
        }
        for (int i = 1; i <= 16; i++) {
            if (i == 12 || i == 14) {
                continue; // not defined as an ISO-8859 part
            }
            if (name.equals("ISO-8859-" + i)) {
                return i == 1 ? 1 : i + 2; // ECI 1 = ISO-8859-1, ECI 4 = ISO-8859-2, ...
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
}
