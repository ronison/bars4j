package org.bars4j.encode.matrix.datamatrix;

import java.util.Arrays;

import org.bars4j.encode.api.UnsupportedTextException;

/**
 * High-level encodation for ECC200, using the ASCII scheme of ISO/IEC 16022 Annex P.
 *
 * <p>The ASCII scheme represents every ISO-8859-1 byte and is always valid. A single character
 * normally becomes one codeword; three rules adjust that: a run of two decimal digits collapses
 * into one codeword (values 130-229), bytes 128-255 are escaped by a preceding upper-shift
 * codeword (235), and the trailing filler consists of the pad codeword (129) followed by a
 * 253-state pseudo-random sequence that spreads the filler across the symbol.
 *
 * <p>Other encodation schemes (C40, Text, X12, EDIFACT, Base256) are intentionally omitted: they
 * only improve compactness and are never required for correctness.
 */
public final class DataMatrixHighLevelEncoder {

    private static final int PAD_CODEWORD = 129;
    private static final int UPPER_SHIFT = 235;
    private static final int DIGIT_PAIR_BASE = 130;
    private static final int FILLER_MULTIPLIER = 149;
    private static final int FILLER_MODULUS = 253;
    private static final int FILLER_LIMIT = 254;

    private DataMatrixHighLevelEncoder() {
    }

    /**
     * Encodes the text into Data Matrix codewords without the final filler. The length returned
     * determines which symbol is selected.
     */
    public static int[] encodeToCodewords(String text) throws UnsupportedTextException {
        if (text.isEmpty()) {
            throw new UnsupportedTextException("Data Matrix requires a non-empty message");
        }
        int[] codewords = new int[2 * text.length()];
        int count = 0;
        int index = 0;
        while (index < text.length()) {
            char current = text.charAt(index);
            if (current > 0xFF) {
                throw new UnsupportedTextException(
                        "Data Matrix ASCII encodation supports ISO-8859-1 characters only, found '"
                                + current + "'");
            }
            char next = index + 1 < text.length() ? text.charAt(index + 1) : '\0';
            if (isDecimalDigit(current) && isDecimalDigit(next)) {
                codewords[count++] = DIGIT_PAIR_BASE
                        + 10 * (current - '0') + (next - '0');
                index += 2;
            } else if (current >= 128) {
                codewords[count++] = UPPER_SHIFT;
                codewords[count++] = current - 128 + 1;
                index += 1;
            } else {
                codewords[count++] = current + 1;
                index += 1;
            }
        }
        return Arrays.copyOf(codewords, count);
    }

    /**
     * Fills the codeword sequence up to the symbol capacity using the ECC200 pad codeword and the
     * 253-state randomisation algorithm.
     *
     * @param codewords the unpadded codewords
     * @param capacity  the symbol's data capacity
     * @return a new array of exactly {@code capacity} codewords
     */
    public static int[] padToCapacity(int[] codewords, int capacity) {
        int[] padded = new int[capacity];
        System.arraycopy(codewords, 0, padded, 0, codewords.length);
        if (codewords.length < capacity) {
            padded[codewords.length] = PAD_CODEWORD;
            for (int position = codewords.length + 2; position <= capacity; position++) {
                padded[position - 1] = fillerCodeword(position);
            }
        }
        return padded;
    }

    /** Computes the randomised pad codeword for a given one-based codeword position. */
    private static int fillerCodeword(int position) {
        int pseudoRandom = (FILLER_MULTIPLIER * position) % FILLER_MODULUS + 1;
        int value = PAD_CODEWORD + pseudoRandom;
        return value <= FILLER_LIMIT ? value : value - FILLER_LIMIT;
    }

    private static boolean isDecimalDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
