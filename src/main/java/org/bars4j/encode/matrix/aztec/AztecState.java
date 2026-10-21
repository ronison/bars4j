package org.bars4j.encode.matrix.aztec;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable snapshot of the encoder while walking through the input: the accumulated token list,
 * the current mode (or the mode to resume after a binary shift), the number of trailing bytes still
 * to be written in binary shift mode, and the total number of bits produced so far.
 */
final class AztecState {

    /** Initial state: empty output, Upper mode, no pending binary shift. */
    static final AztecState INITIAL_STATE =
            new AztecState(AztecToken.EMPTY, AztecHighLevelEncoder.MODE_UPPER, 0, 0);

    private final int mode;
    private final AztecToken token;
    private final int binaryShiftByteCount;
    private final int bitCount;
    private final int binaryShiftCost;

    private AztecState(AztecToken token, int mode, int binaryShiftByteCount, int bitCount) {
        this.token = token;
        this.mode = mode;
        this.binaryShiftByteCount = binaryShiftByteCount;
        this.bitCount = bitCount;
        this.binaryShiftCost = binaryShiftCost(binaryShiftByteCount);
    }

    int mode() {
        return mode;
    }

    AztecToken token() {
        return token;
    }

    int binaryShiftByteCount() {
        return binaryShiftByteCount;
    }

    int bitCount() {
        return bitCount;
    }

    /** Appends the {@code FLG(n)} escape that announces an ECI (or FNC1 when {@code eci < 0}). */
    AztecState appendFLGn(int eci) {
        AztecState result = shiftAndAppend(AztecHighLevelEncoder.MODE_PUNCT, 0); // 0: FLG(n)
        AztecToken token = result.token;
        int bitsAdded = 3;
        if (eci < 0) {
            token = token.add(0, 3); // 0: FNC1
        } else if (eci > 999999) {
            throw new IllegalArgumentException("ECI code must be between 0 and 999999");
        } else {
            byte[] eciDigits = Integer.toString(eci).getBytes(StandardCharsets.ISO_8859_1);
            token = token.add(eciDigits.length, 3); // 1-6: number of ECI digits
            for (byte eciDigit : eciDigits) {
                token = token.add(eciDigit - '0' + 2, 4);
            }
            bitsAdded += eciDigits.length * 4;
        }
        return new AztecState(token, mode, 0, bitCount + bitsAdded);
    }

    /** Latches to {@code mode} (if needed) and appends {@code value}. */
    AztecState latchAndAppend(int mode, int value) {
        int newBitCount = this.bitCount;
        AztecToken newToken = this.token;
        if (mode != this.mode) {
            int latch = AztecHighLevelEncoder.LATCH_TABLE[this.mode][mode];
            newToken = newToken.add(latch & 0xFFFF, latch >> 16);
            newBitCount += latch >> 16;
        }
        int codeBitCount = mode == AztecHighLevelEncoder.MODE_DIGIT ? 4 : 5;
        newToken = newToken.add(value, codeBitCount);
        return new AztecState(newToken, mode, 0, newBitCount + codeBitCount);
    }

    /** Temporarily shifts to {@code mode} to append a single {@code value}. */
    AztecState shiftAndAppend(int mode, int value) {
        AztecToken newToken = this.token;
        int thisModeBitCount = this.mode == AztecHighLevelEncoder.MODE_DIGIT ? 4 : 5;
        newToken = newToken.add(AztecHighLevelEncoder.SHIFT_TABLE[this.mode][mode], thisModeBitCount);
        newToken = newToken.add(value, 5);
        return new AztecState(newToken, this.mode, 0, this.bitCount + thisModeBitCount + 5);
    }

    /** Extends the pending binary shift run by the byte at {@code index}. */
    AztecState addBinaryShiftChar(int index) {
        AztecToken newToken = this.token;
        int newMode = this.mode;
        int newBitCount = this.bitCount;
        if (this.mode == AztecHighLevelEncoder.MODE_PUNCT || this.mode == AztecHighLevelEncoder.MODE_DIGIT) {
            int latch = AztecHighLevelEncoder.LATCH_TABLE[newMode][AztecHighLevelEncoder.MODE_UPPER];
            newToken = newToken.add(latch & 0xFFFF, latch >> 16);
            newBitCount += latch >> 16;
            newMode = AztecHighLevelEncoder.MODE_UPPER;
        }
        int deltaBitCount =
                (binaryShiftByteCount == 0 || binaryShiftByteCount == 31) ? 18
                        : (binaryShiftByteCount == 62) ? 9 : 8;
        AztecState result = new AztecState(newToken, newMode, binaryShiftByteCount + 1, newBitCount + deltaBitCount);
        if (result.binaryShiftByteCount == 2047 + 31) {
            // The run has reached its maximum length; close it now.
            result = result.endBinaryShift(index + 1);
        }
        return result;
    }

    /** Closes any pending binary shift run, fixing its start index. */
    AztecState endBinaryShift(int index) {
        if (binaryShiftByteCount == 0) {
            return this;
        }
        AztecToken newToken = this.token.addBinaryShift(index - binaryShiftByteCount, binaryShiftByteCount);
        return new AztecState(newToken, mode, 0, this.bitCount);
    }

    /**
     * Returns {@code true} if this state is at least as good as {@code other} whatever follows,
     * allowing the search to discard dominated states.
     */
    boolean isBetterThanOrEqualTo(AztecState other) {
        int newModeBitCount =
                this.bitCount + (AztecHighLevelEncoder.LATCH_TABLE[this.mode][other.mode] >> 16);
        if (this.binaryShiftByteCount < other.binaryShiftByteCount) {
            newModeBitCount += other.binaryShiftCost - this.binaryShiftCost;
        } else if (this.binaryShiftByteCount > other.binaryShiftByteCount && other.binaryShiftByteCount > 0) {
            // Worst case: this state crosses the 31-byte boundary that the other avoids.
            newModeBitCount += 10;
        }
        return newModeBitCount <= other.bitCount;
    }

    /** Serialises the accumulated tokens into a bit buffer. */
    AztecBitBuffer toBitArray(byte[] text) {
        List<AztecToken> symbols = new ArrayList<>();
        for (AztecToken token = endBinaryShift(text.length).token; token != null; token = token.previous()) {
            symbols.add(token);
        }
        AztecBitBuffer buffer = new AztecBitBuffer();
        for (int i = symbols.size() - 1; i >= 0; i--) {
            symbols.get(i).appendTo(buffer, text);
        }
        return buffer;
    }

    @Override
    public String toString() {
        return String.format("%s bits=%d bytes=%d",
                AztecHighLevelEncoder.MODE_NAMES[mode], bitCount, binaryShiftByteCount);
    }

    private static int binaryShiftCost(int byteCount) {
        if (byteCount > 62) {
            return 21; // binary shift with extended length
        }
        if (byteCount > 31) {
            return 20; // two binary shifts
        }
        if (byteCount > 0) {
            return 10; // one binary shift
        }
        return 0;
    }
}
