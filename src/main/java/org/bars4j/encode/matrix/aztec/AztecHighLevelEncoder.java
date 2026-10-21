package org.bars4j.encode.matrix.aztec;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Turns the input bytes into the first level of Aztec encoding: the sequence of mode latches,
 * temporary shifts and binary shift runs that represent the text.
 *
 * <p>For each input prefix a set of candidate {@link AztecState}s is maintained. Adding one more
 * character branches every state into the possible encodings (latch, shift or binary shift), and
 * dominated states are pruned before continuing. Finally the cheapest surviving state is serialised.
 */
public final class AztecHighLevelEncoder {

    static final int MODE_UPPER = 0;
    static final int MODE_LOWER = 1;
    static final int MODE_DIGIT = 2;
    static final int MODE_MIXED = 3;
    static final int MODE_PUNCT = 4;

    static final String[] MODE_NAMES = {"UPPER", "LOWER", "DIGIT", "MIXED", "PUNCT"};

    /**
     * For each ordered pair of modes, packs the cheapest way to latch from the first to the second:
     * the high 16 bits hold the number of bits, the low 16 bits the latch code itself.
     */
    static final int[][] LATCH_TABLE = {
            { // UPPER
                    0,
                    (5 << 16) + 28,               // UPPER -> LOWER
                    (5 << 16) + 30,               // UPPER -> DIGIT
                    (5 << 16) + 29,               // UPPER -> MIXED
                    (10 << 16) + (29 << 5) + 30,  // UPPER -> MIXED -> PUNCT
            },
            { // LOWER
                    (9 << 16) + (30 << 4) + 14,   // LOWER -> DIGIT -> UPPER
                    0,
                    (5 << 16) + 30,               // LOWER -> DIGIT
                    (5 << 16) + 29,               // LOWER -> MIXED
                    (10 << 16) + (29 << 5) + 30,  // LOWER -> MIXED -> PUNCT
            },
            { // DIGIT
                    (4 << 16) + 14,               // DIGIT -> UPPER
                    (9 << 16) + (14 << 5) + 28,   // DIGIT -> UPPER -> LOWER
                    0,
                    (9 << 16) + (14 << 5) + 29,   // DIGIT -> UPPER -> MIXED
                    (14 << 16) + (14 << 10) + (29 << 5) + 30, // DIGIT -> UPPER -> MIXED -> PUNCT
            },
            { // MIXED
                    (5 << 16) + 29,               // MIXED -> UPPER
                    (5 << 16) + 28,               // MIXED -> LOWER
                    (10 << 16) + (29 << 5) + 30,  // MIXED -> UPPER -> DIGIT
                    0,
                    (5 << 16) + 30,               // MIXED -> PUNCT
            },
            { // PUNCT
                    (5 << 16) + 31,               // PUNCT -> UPPER
                    (10 << 16) + (31 << 5) + 28,  // PUNCT -> UPPER -> LOWER
                    (10 << 16) + (31 << 5) + 30,  // PUNCT -> UPPER -> DIGIT
                    (10 << 16) + (31 << 5) + 29,  // PUNCT -> UPPER -> MIXED
                    0,
            },
    };

    /** {@code [mode][byte]} -> code for that byte in that mode, or 0 when the byte is not defined. */
    private static final int[][] CHAR_MAP = new int[5][256];
    static {
        CHAR_MAP[MODE_UPPER][' '] = 1;
        for (int c = 'A'; c <= 'Z'; c++) {
            CHAR_MAP[MODE_UPPER][c] = c - 'A' + 2;
        }
        CHAR_MAP[MODE_LOWER][' '] = 1;
        for (int c = 'a'; c <= 'z'; c++) {
            CHAR_MAP[MODE_LOWER][c] = c - 'a' + 2;
        }
        CHAR_MAP[MODE_DIGIT][' '] = 1;
        for (int c = '0'; c <= '9'; c++) {
            CHAR_MAP[MODE_DIGIT][c] = c - '0' + 2;
        }
        CHAR_MAP[MODE_DIGIT][','] = 12;
        CHAR_MAP[MODE_DIGIT]['.'] = 13;

        int[] mixedTable = {
                '\0', ' ', '\1', '\2', '\3', '\4', '\5', '\6', '\7', '\b', '\t', '\n',
                '\13', '\f', '\r', '\33', '\34', '\35', '\36', '\37', '@', '\\', '^',
                '_', '`', '|', '~', '\177',
        };
        for (int i = 0; i < mixedTable.length; i++) {
            CHAR_MAP[MODE_MIXED][mixedTable[i]] = i;
        }

        int[] punctTable = {
                '\0', '\r', '\0', '\0', '\0', '\0', '!', '"', '#', '$', '%', '&', '\'',
                '(', ')', '*', '+', ',', '-', '.', '/', ':', ';', '<', '=', '>', '?',
                '[', ']', '{', '}',
        };
        for (int i = 0; i < punctTable.length; i++) {
            if (punctTable[i] > 0) {
                CHAR_MAP[MODE_PUNCT][punctTable[i]] = i;
            }
        }
    }

    /** {@code [current mode][target mode]} -> shift code, or -1 when no shift is available. */
    static final int[][] SHIFT_TABLE = new int[6][6];
    static {
        for (int[] table : SHIFT_TABLE) {
            Arrays.fill(table, -1);
        }
        SHIFT_TABLE[MODE_UPPER][MODE_PUNCT] = 0;

        SHIFT_TABLE[MODE_LOWER][MODE_PUNCT] = 0;
        SHIFT_TABLE[MODE_LOWER][MODE_UPPER] = 28;

        SHIFT_TABLE[MODE_MIXED][MODE_PUNCT] = 0;

        SHIFT_TABLE[MODE_DIGIT][MODE_PUNCT] = 0;
        SHIFT_TABLE[MODE_DIGIT][MODE_UPPER] = 15;
    }

    private final byte[] text;
    private final Charset charset;

    public AztecHighLevelEncoder(byte[] text) {
        this(text, null);
    }

    public AztecHighLevelEncoder(byte[] text, Charset charset) {
        this.text = text;
        this.charset = charset;
    }

    /** Encodes the input into a bit buffer. */
    public AztecBitBuffer encode() {
        AztecState initialState = AztecState.INITIAL_STATE;
        if (charset != null) {
            initialState = initialState.appendFLGn(AztecEci.valueFor(charset));
        }
        List<AztecState> states = new ArrayList<>();
        states.add(initialState);
        for (int index = 0; index < text.length; index++) {
            int pairCode = 0;
            int nextChar = index + 1 < text.length ? text[index + 1] : 0;
            switch (text[index]) {
                case '\r':
                    pairCode = nextChar == '\n' ? 2 : 0;
                    break;
                case '.':
                    pairCode = nextChar == ' ' ? 3 : 0;
                    break;
                case ',':
                    pairCode = nextChar == ' ' ? 4 : 0;
                    break;
                case ':':
                    pairCode = nextChar == ' ' ? 5 : 0;
                    break;
                default:
                    pairCode = 0;
            }
            if (pairCode > 0) {
                // One of the four special PUNCT pairs; consume both characters at once.
                states = updateStateListForPair(states, index, pairCode);
                index++;
            } else {
                states = updateStateListForChar(states, index);
            }
        }
        AztecState minState = Collections.min(states, Comparator.comparingInt(AztecState::bitCount));
        return minState.toBitArray(text);
    }

    private List<AztecState> updateStateListForChar(List<AztecState> states, int index) {
        List<AztecState> result = new LinkedList<>();
        for (AztecState state : states) {
            updateStateForChar(state, index, result);
        }
        return simplifyStates(result);
    }

    private void updateStateForChar(AztecState state, int index, List<AztecState> result) {
        char ch = (char) (text[index] & 0xFF);
        boolean charInCurrentTable = CHAR_MAP[state.mode()][ch] > 0;
        AztecState stateNoBinary = null;
        for (int mode = 0; mode <= MODE_PUNCT; mode++) {
            int charInMode = CHAR_MAP[mode][ch];
            if (charInMode > 0) {
                if (stateNoBinary == null) {
                    // Close any binary shift only once, the first time it is actually required.
                    stateNoBinary = state.endBinaryShift(index);
                }
                if (!charInCurrentTable || mode == state.mode() || mode == MODE_DIGIT) {
                    // If the character exists in the current table, latching elsewhere could only
                    // be justified for DIGIT, which costs fewer bits.
                    result.add(stateNoBinary.latchAndAppend(mode, charInMode));
                }
                if (!charInCurrentTable && SHIFT_TABLE[state.mode()][mode] >= 0) {
                    // Shifting into a mode that already holds the character can never save bits.
                    result.add(stateNoBinary.shiftAndAppend(mode, charInMode));
                }
            }
        }
        if (state.binaryShiftByteCount() > 0 || CHAR_MAP[state.mode()][ch] == 0) {
            // Starting a binary shift is only worthwhile when already in one, or when the character
            // is unavailable in the current mode.
            result.add(state.addBinaryShiftChar(index));
        }
    }

    private static List<AztecState> updateStateListForPair(List<AztecState> states, int index, int pairCode) {
        List<AztecState> result = new LinkedList<>();
        for (AztecState state : states) {
            updateStateForPair(state, index, pairCode, result);
        }
        return simplifyStates(result);
    }

    private static void updateStateForPair(AztecState state, int index, int pairCode, List<AztecState> result) {
        AztecState stateNoBinary = state.endBinaryShift(index);
        // Possibility 1: latch to PUNCT and emit the pair code.
        result.add(stateNoBinary.latchAndAppend(MODE_PUNCT, pairCode));
        if (state.mode() != MODE_PUNCT) {
            // Possibility 2: shift to PUNCT. Every mode but PUNCT itself can shift.
            result.add(stateNoBinary.shiftAndAppend(MODE_PUNCT, pairCode));
        }
        if (pairCode == 3 || pairCode == 4) {
            // ". " and ", " can also be written as two DIGIT characters.
            result.add(stateNoBinary
                    .latchAndAppend(MODE_DIGIT, 16 - pairCode)
                    .latchAndAppend(MODE_DIGIT, 1));
        }
        if (state.binaryShiftByteCount() > 0) {
            // Only worth it if the pair is already inside a binary shift run.
            result.add(state.addBinaryShiftChar(index).addBinaryShiftChar(index + 1));
        }
    }

    /** Removes states that are never better than another state in the list. */
    private static List<AztecState> simplifyStates(List<AztecState> states) {
        Deque<AztecState> result = new LinkedList<>();
        for (AztecState newState : states) {
            boolean add = true;
            for (Iterator<AztecState> iterator = result.iterator(); iterator.hasNext(); ) {
                AztecState oldState = iterator.next();
                if (oldState.isBetterThanOrEqualTo(newState)) {
                    add = false;
                    break;
                }
                if (newState.isBetterThanOrEqualTo(oldState)) {
                    iterator.remove();
                }
            }
            if (add) {
                result.addFirst(newState);
            }
        }
        return new ArrayList<>(result);
    }
}
