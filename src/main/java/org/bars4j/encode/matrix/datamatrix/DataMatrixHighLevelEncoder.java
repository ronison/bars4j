package org.bars4j.encode.matrix.datamatrix;

import org.bars4j.encode.api.UnsupportedTextException;

/**
 * High level encoding for Data Matrix ECC200.
 *
 * <p>This implementation uses the ASCII encodation scheme (ISO/IEC 16022 Annex P). Every byte in
 * the range 0-255 is representable: values 0-127 map to codewords {@code value + 1}, values 128-255
 * use the upper-shift (codeword 235) followed by {@code value - 127}, and pairs of digits are
 * compacted into a single codeword (130-229). Other encodation modes (C40, Text, X12, EDIFACT,
 * Base256) are intentionally out of scope: ASCII is always valid, just not always the most compact.
 */
public final class DataMatrixHighLevelEncoder {

    private static final int PAD = 129;
    private static final int UPPER_SHIFT = 235;

    private DataMatrixHighLevelEncoder() {
    }

    /**
     * Encodes the text into Data Matrix codewords, <em>without</em> final padding. The returned
     * length drives symbol selection.
     */
    public static int[] encodeToCodewords(String text) throws UnsupportedTextException {
        if (text.isEmpty()) {
            throw new UnsupportedTextException("Data Matrix requires a non-empty message");
        }
        int[] codewords = new int[text.length() * 2];
        int count = 0;
        int p = 0;
        while (p < text.length()) {
            char c = text.charAt(p);
            if (c > 255) {
                throw new UnsupportedTextException(
                        "Data Matrix ASCII encodation only supports ISO-8859-1 characters, found: '"
                                + c + "'");
            }
            if (p + 1 < text.length() && isDigit(c) && isDigit(text.charAt(p + 1))) {
                codewords[count++] = (c - '0') * 10 + (text.charAt(p + 1) - '0') + 130;
                p += 2;
            } else if (c >= 128) {
                codewords[count++] = UPPER_SHIFT;
                codewords[count++] = c - 128 + 1;
                p++;
            } else {
                codewords[count++] = c + 1;
                p++;
            }
        }
        int[] result = new int[count];
        System.arraycopy(codewords, 0, result, 0, count);
        return result;
    }

    /**
     * Pads the codewords to the symbol capacity using the ECC200 pad codeword followed by the
     * 253-state randomisation algorithm.
     */
    public static int[] padToCapacity(int[] codewords, int capacity) {
        int[] result = new int[capacity];
        System.arraycopy(codewords, 0, result, 0, codewords.length);
        if (codewords.length < capacity) {
            result[codewords.length] = PAD;
            for (int i = codewords.length + 1; i < capacity; i++) {
                result[i] = randomize253State(i + 1);
            }
        }
        return result;
    }

    private static int randomize253State(int codewordPosition) {
        int pseudoRandom = ((149 * codewordPosition) % 253) + 1;
        int tempVariable = PAD + pseudoRandom;
        return tempVariable <= 254 ? tempVariable : tempVariable - 254;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
