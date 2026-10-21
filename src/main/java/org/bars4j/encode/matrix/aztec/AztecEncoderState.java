package org.bars4j.encode.matrix.aztec;

import java.util.ArrayList;
import java.util.List;

/**
 * One candidate reached by the Aztec high-level encoder.
 *
 * <p>A state is immutable and records four things: the mode the stream is currently left in, the
 * number of pending bytes belonging to a binary-shift run that has been started but not yet closed,
 * the total number of bits emitted so far, and a backwards-linked chain of the output steps.
 *
 * <p>The chain is shared between states, so branching the search costs only the newly appended
 * steps. A step is either a fixed-width mode code or a run of raw input bytes emitted through a
 * binary shift.
 */
final class AztecEncoderState {

    /** Longest binary-shift run that the encoding can describe (31 implicit + 2047 explicit bytes). */
    private static final int MAX_SHIFT_BYTES = 2078;

    /** A single already-decided output fragment. */
    private static final class Step {
        final Step previous;
        final boolean rawBytes;
        /** For a code: its value. For raw bytes: index of the first byte. */
        final int value;
        /** For a code: its width in bits. For raw bytes: the number of bytes. */
        final int width;

        Step(Step previous, boolean rawBytes, int value, int width) {
            this.previous = previous;
            this.rawBytes = rawBytes;
            this.value = value;
            this.width = width;
        }
    }

    private final Step steps;
    private final AztecMode mode;
    private final int pendingShiftBytes;
    private final int bitCount;
    private final int shiftCost;

    private AztecEncoderState(Step steps, AztecMode mode, int pendingShiftBytes, int bitCount) {
        this.steps = steps;
        this.mode = mode;
        this.pendingShiftBytes = pendingShiftBytes;
        this.bitCount = bitCount;
        this.shiftCost = binaryShiftCost(pendingShiftBytes);
    }

    /** Returns the empty state: nothing emitted, in UPPER mode. */
    static AztecEncoderState initial() {
        return new AztecEncoderState(null, AztecMode.UPPER, 0, 0);
    }

    AztecMode mode() {
        return mode;
    }

    int pendingShiftBytes() {
        return pendingShiftBytes;
    }

    int bitCount() {
        return bitCount;
    }

    /**
     * Emits the {@code FLG(n)} escape used to announce an ECI. A negative {@code eci} selects the
     * FNC1 flag instead of a digit string.
     */
    AztecEncoderState announceCharset(int eci) {
        // FLG(n) is PUNCT code 0; a temporary shift into PUNCT keeps the current mode.
        AztecEncoderState shifted = appendWithShift(AztecMode.PUNCT, 0);
        if (eci < 0) {
            return shifted.append(0, 3);
        }
        if (eci > 999999) {
            throw new IllegalArgumentException("ECI code must be between 0 and 999999");
        }
        String digits = Integer.toString(eci);
        AztecEncoderState state = shifted.append(digits.length(), 3);
        for (int i = 0; i < digits.length(); i++) {
            state = state.append(digits.charAt(i) - '0' + 2, 4);
        }
        return state;
    }

    /** Latches permanently to {@code target} (if needed) and appends {@code value} there. */
    AztecEncoderState switchAndAppend(AztecMode target, int value) {
        Step newSteps = steps;
        int newBits = bitCount;
        if (target != mode) {
            newSteps = latch(newSteps, mode, target);
            newBits += AztecModeTables.latchBitCount(mode, target);
        }
        newSteps = new Step(newSteps, false, value, target.codeWidth());
        return new AztecEncoderState(newSteps, target, 0, newBits + target.codeWidth());
    }

    /** Temporarily shifts to {@code target} for one {@code value} and returns to the current mode. */
    AztecEncoderState appendWithShift(AztecMode target, int value) {
        int shiftWidth = mode.codeWidth();
        Step shifted = new Step(steps, false, AztecModeTables.shiftCode(mode, target), shiftWidth);
        Step code = new Step(shifted, false, value, 5);
        return new AztecEncoderState(code, mode, 0, bitCount + shiftWidth + 5);
    }

    /** Extends the open binary-shift run by the input byte at {@code index}. */
    AztecEncoderState addShiftedByte(int index) {
        Step newSteps = steps;
        AztecMode newMode = mode;
        int newBits = bitCount;
        if (mode == AztecMode.PUNCT || mode == AztecMode.DIGIT) {
            // Binary shift is not directly available from these modes; latch to UPPER first.
            newSteps = latch(newSteps, mode, AztecMode.UPPER);
            newBits += AztecModeTables.latchBitCount(mode, AztecMode.UPPER);
            newMode = AztecMode.UPPER;
        }
        // The first byte and byte 31 of a short run open a header; byte 63 switches to the extended
        // length header, which is smaller than starting a second run.
        int deltaBits = (pendingShiftBytes == 0 || pendingShiftBytes == 31) ? 18
                : (pendingShiftBytes == 62) ? 9 : 8;
        AztecEncoderState next =
                new AztecEncoderState(newSteps, newMode, pendingShiftBytes + 1, newBits + deltaBits);
        if (next.pendingShiftBytes == MAX_SHIFT_BYTES) {
            next = next.closeShift(index + 1);
        }
        return next;
    }

    /** Closes any open binary-shift run, recording the range of input bytes it covers. */
    AztecEncoderState closeShift(int index) {
        if (pendingShiftBytes == 0) {
            return this;
        }
        Step run = new Step(steps, true, index - pendingShiftBytes, pendingShiftBytes);
        return new AztecEncoderState(run, mode, 0, bitCount);
    }

    /**
     * Returns {@code true} if this state can be no worse than {@code other} for every possible
     * continuation, so that {@code other} may be discarded. The comparison anticipates the latch
     * needed to reach the other state's mode and the worst-case cost of closing or extending the
     * pending binary-shift runs.
     */
    boolean dominates(AztecEncoderState other) {
        int projected = bitCount + AztecModeTables.latchBitCount(mode, other.mode);
        if (pendingShiftBytes < other.pendingShiftBytes) {
            projected += other.shiftCost - shiftCost;
        } else if (pendingShiftBytes > other.pendingShiftBytes && other.pendingShiftBytes > 0) {
            // This state carries more open shift bytes and may have to pay for an extra header.
            projected += 10;
        }
        return projected <= other.bitCount;
    }

    /** Serialises the chain of steps, reading the raw bytes of a binary shift from {@code input}. */
    AztecBitBuffer toBitBuffer(byte[] input) {
        List<Step> reversed = new ArrayList<>();
        for (Step step = closeShift(input.length).steps; step != null; step = step.previous) {
            reversed.add(step);
        }
        AztecBitBuffer out = new AztecBitBuffer();
        for (int i = reversed.size() - 1; i >= 0; i--) {
            Step step = reversed.get(i);
            if (step.rawBytes) {
                writeBinaryShift(out, input, step.value, step.width);
            } else {
                out.appendBits(step.value, step.width);
            }
        }
        return out;
    }

    /** Appends a code without changing the mode or disturbing a pending shift. */
    private AztecEncoderState append(int value, int width) {
        Step step = new Step(steps, false, value, width);
        return new AztecEncoderState(step, mode, 0, bitCount + width);
    }

    /** Pushes the codes of a latch onto a step chain, in emission order. */
    private static Step latch(Step head, AztecMode from, AztecMode to) {
        int[] codes = AztecModeTables.latchCodes(from, to);
        Step result = head;
        for (int i = 0; i < codes.length; i += 2) {
            result = new Step(result, false, codes[i], codes[i + 1]);
        }
        return result;
    }

    private static void writeBinaryShift(AztecBitBuffer out, byte[] input, int start, int byteCount) {
        for (int i = 0; i < byteCount; i++) {
            if (i == 0 || (i == 31 && byteCount <= 62)) {
                // A binary-shift header: the B/S code, then a length field. Runs longer than 62
                // bytes use the 16-bit extended length; otherwise a five-bit length follows the
                // B/S code, and a second header opens byte 31 when needed.
                out.appendBits(31, 5);
                if (byteCount > 62) {
                    out.appendBits(byteCount - 31, 16);
                } else if (i == 0) {
                    out.appendBits(Math.min(byteCount, 31), 5);
                } else {
                    out.appendBits(byteCount - 31, 5);
                }
            }
            out.appendBits(input[start + i] & 0xFF, 8);
        }
    }

    /** Rough number of header bits an open run of {@code byteCount} bytes will need. */
    private static int binaryShiftCost(int byteCount) {
        if (byteCount > 62) {
            return 21;
        }
        if (byteCount > 31) {
            return 20;
        }
        return byteCount > 0 ? 10 : 0;
    }
}
