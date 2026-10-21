package org.bars4j.encode.matrix.aztec;

import java.util.Arrays;

/**
 * The mode switch and character tables that drive the Aztec encodation.
 *
 * <p>Everything here is data taken from ISO/IEC 24778: the character code of each byte in each mode
 * (referred to as the character set), the sequence of codes needed to latch from one mode to
 * another, and the single codes available as temporary shifts.
 */
final class AztecModeTables {

    private static final int MODES = AztecMode.values().length;

    /** {@code characterCode[mode][byte value]} is the mode code, or 0 when the byte has none. */
    private static final int[][] CHARACTER_CODE = new int[MODES][256];

    /**
     * {@code latch[mode][otherMode]} holds the codes emitted to switch permanently from one mode to
     * another. Each code is stored as two consecutive entries {@code value, width}; a missing entry
     * means no switch is needed (both modes are equal).
     */
    private static final int[][][] LATCH = new int[MODES][MODES][];

    /** Total number of bits of each latch, precomputed for the search's cost comparisons. */
    private static final int[][] LATCH_BITS = new int[MODES][MODES];

    /** {@code shift[mode][otherMode]} is the temporary-shift code, or -1 when none exists. */
    private static final int[][] SHIFT = new int[MODES][MODES];

    static {
        buildCharacterSets();
        buildLatches();
        buildShifts();

        for (int from = 0; from < MODES; from++) {
            for (int to = 0; to < MODES; to++) {
                int bits = 0;
                int[] codes = LATCH[from][to];
                for (int i = 1; i < codes.length; i += 2) {
                    bits += codes[i];
                }
                LATCH_BITS[from][to] = bits;
            }
        }
    }

    private AztecModeTables() {
    }

    private static void buildCharacterSets() {
        // UPPER: blank and A-Z.
        CHARACTER_CODE[AztecMode.UPPER.ordinal()][' '] = 1;
        for (int c = 'A'; c <= 'Z'; c++) {
            CHARACTER_CODE[AztecMode.UPPER.ordinal()][c] = c - 'A' + 2;
        }

        // LOWER: blank and a-z.
        CHARACTER_CODE[AztecMode.LOWER.ordinal()][' '] = 1;
        for (int c = 'a'; c <= 'z'; c++) {
            CHARACTER_CODE[AztecMode.LOWER.ordinal()][c] = c - 'a' + 2;
        }

        // DIGIT: blank, 0-9, comma and full stop. Code 14/15 are the U/L and U/S switches.
        CHARACTER_CODE[AztecMode.DIGIT.ordinal()][' '] = 1;
        for (int c = '0'; c <= '9'; c++) {
            CHARACTER_CODE[AztecMode.DIGIT.ordinal()][c] = c - '0' + 2;
        }
        CHARACTER_CODE[AztecMode.DIGIT.ordinal()][','] = 12;
        CHARACTER_CODE[AztecMode.DIGIT.ordinal()]['.'] = 13;

        // MIXED: the C0 controls that have no dedicated mode, then a handful of ASCII signs. The
        // codes are represented by their numeric value to avoid octal-escape confusion.
        int[] mixedCharacters = {
                0x00, ' ', 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B,
                0x0C, 0x0D, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F,
                '@', '\\', '^', '_', '`', '|', '~', 0x7F,
        };
        for (int code = 0; code < mixedCharacters.length; code++) {
            if (code != 0) {
                CHARACTER_CODE[AztecMode.MIXED.ordinal()][mixedCharacters[code]] = code;
            }
        }

        // PUNCT: CR, the punctuation signs, and the bracket pairs. Codes 2-5 are the two-character
        // sequences CR LF, ". ", ", " and ": ", which the encoder handles as pairs and which have no
        // single-byte entry here.
        int[] punctuationCharacters = {
                0x00, '\r', 0x00, 0x00, 0x00, 0x00,
                '!', '"', '#', '$', '%', '&', '\'',
                '(', ')', '*', '+', ',', '-', '.', '/', ':', ';', '<', '=', '>', '?',
                '[', ']', '{', '}',
        };
        for (int code = 0; code < punctuationCharacters.length; code++) {
            if (punctuationCharacters[code] != 0) {
                CHARACTER_CODE[AztecMode.PUNCT.ordinal()][punctuationCharacters[code]] = code;
            }
        }
    }

    private static void buildLatches() {
        // Cheapest permanent switches between modes. A switch that needs two latches (for example
        // LOWER to PUNCT through MIXED) is stored as the concatenation of both codes.
        latch(AztecMode.UPPER, AztecMode.LOWER, 28, 5);
        latch(AztecMode.UPPER, AztecMode.DIGIT, 30, 5);
        latch(AztecMode.UPPER, AztecMode.MIXED, 29, 5);
        latch(AztecMode.UPPER, AztecMode.PUNCT, 29, 5, 30, 5);

        latch(AztecMode.LOWER, AztecMode.UPPER, 30, 5, 14, 4);
        latch(AztecMode.LOWER, AztecMode.DIGIT, 30, 5);
        latch(AztecMode.LOWER, AztecMode.MIXED, 29, 5);
        latch(AztecMode.LOWER, AztecMode.PUNCT, 29, 5, 30, 5);

        latch(AztecMode.DIGIT, AztecMode.UPPER, 14, 4);
        latch(AztecMode.DIGIT, AztecMode.LOWER, 14, 4, 28, 5);
        latch(AztecMode.DIGIT, AztecMode.MIXED, 14, 4, 29, 5);
        latch(AztecMode.DIGIT, AztecMode.PUNCT, 14, 4, 29, 5, 30, 5);

        latch(AztecMode.MIXED, AztecMode.UPPER, 29, 5);
        latch(AztecMode.MIXED, AztecMode.LOWER, 28, 5);
        latch(AztecMode.MIXED, AztecMode.DIGIT, 29, 5, 30, 5);
        latch(AztecMode.MIXED, AztecMode.PUNCT, 30, 5);

        latch(AztecMode.PUNCT, AztecMode.UPPER, 31, 5);
        latch(AztecMode.PUNCT, AztecMode.LOWER, 31, 5, 28, 5);
        latch(AztecMode.PUNCT, AztecMode.DIGIT, 31, 5, 30, 5);
        latch(AztecMode.PUNCT, AztecMode.MIXED, 31, 5, 29, 5);

        for (AztecMode mode : AztecMode.values()) {
            LATCH[mode.ordinal()][mode.ordinal()] = new int[0];
        }
    }

    private static void latch(AztecMode from, AztecMode to, int... valueWidthPairs) {
        LATCH[from.ordinal()][to.ordinal()] = valueWidthPairs;
    }

    private static void buildShifts() {
        for (int[] row : SHIFT) {
            Arrays.fill(row, -1);
        }
        // Only a few temporary shifts are defined: they lend the next code of another mode without
        // leaving the current one.
        SHIFT[AztecMode.UPPER.ordinal()][AztecMode.PUNCT.ordinal()] = 0;
        SHIFT[AztecMode.LOWER.ordinal()][AztecMode.PUNCT.ordinal()] = 0;
        SHIFT[AztecMode.LOWER.ordinal()][AztecMode.UPPER.ordinal()] = 28;
        SHIFT[AztecMode.MIXED.ordinal()][AztecMode.PUNCT.ordinal()] = 0;
        SHIFT[AztecMode.DIGIT.ordinal()][AztecMode.PUNCT.ordinal()] = 0;
        SHIFT[AztecMode.DIGIT.ordinal()][AztecMode.UPPER.ordinal()] = 15;
    }

    /** Returns the code for {@code byteValue} in {@code mode}, or 0 when it has none. */
    static int characterCode(AztecMode mode, int byteValue) {
        return CHARACTER_CODE[mode.ordinal()][byteValue];
    }

    /** Returns the {@code value, width} pairs of the latch from {@code from} to {@code to}. */
    static int[] latchCodes(AztecMode from, AztecMode to) {
        return LATCH[from.ordinal()][to.ordinal()];
    }

    /** Returns the number of bits of the latch from {@code from} to {@code to}. */
    static int latchBitCount(AztecMode from, AztecMode to) {
        return LATCH_BITS[from.ordinal()][to.ordinal()];
    }

    /** Returns the temporary-shift code from {@code from} to {@code to}, or -1 when unavailable. */
    static int shiftCode(AztecMode from, AztecMode to) {
        return SHIFT[from.ordinal()][to.ordinal()];
    }
}
