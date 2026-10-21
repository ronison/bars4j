package org.bars4j.encode.matrix.aztec;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * First level of Aztec encoding: converts the raw input bytes into the bit stream of mode codes,
 * temporary shifts and binary-shift runs.
 *
 * <p>The stream is grown one input symbol at a time while a frontier of candidate
 * {@link AztecEncoderState}s is kept. Every candidate can branch into the encodings allowed at the
 * next byte (a latch into a mode that holds it, a shift, or a binary shift), and candidates that can
 * never win are pruned. When the input is exhausted the cheapest surviving candidate is serialised.
 *
 * <p>Four punctuation symbols are encoded together with their following space or newline as a
 * single two-character code; those pairs are consumed in one step.
 */
public final class AztecHighLevelEncoder {

    private final byte[] input;
    private final Charset charset;

    /** Creates an encoder for {@code input} with no ECI announcement. */
    public AztecHighLevelEncoder(byte[] input) {
        this(input, null);
    }

    /**
     * Creates an encoder for {@code input}. When {@code charset} is non-null an ECI is emitted at
     * the start of the stream.
     */
    public AztecHighLevelEncoder(byte[] input, Charset charset) {
        this.input = input;
        this.charset = charset;
    }

    /** Encodes the input into the first-level bit stream. */
    public AztecBitBuffer encode() {
        AztecEncoderState initial = AztecEncoderState.initial();
        if (charset != null) {
            initial = initial.announceCharset(AztecEci.valueFor(charset));
        }
        List<AztecEncoderState> frontier = new ArrayList<>();
        frontier.add(initial);

        int index = 0;
        while (index < input.length) {
            int pairCode = pairCodeAt(index);
            if (pairCode != 0) {
                frontier = branchOverPair(frontier, index, pairCode);
                index += 2;
            } else {
                frontier = branchOverByte(frontier, index);
                index++;
            }
        }

        AztecEncoderState best =
                Collections.min(frontier, Comparator.comparingInt(AztecEncoderState::bitCount));
        return best.toBitBuffer(input);
    }

    /**
     * Returns the two-character punctuation code starting at {@code index}, or 0 when the byte does
     * not begin one of the four paired symbols.
     */
    private int pairCodeAt(int index) {
        int next = index + 1 < input.length ? input[index + 1] & 0xFF : -1;
        switch (input[index]) {
            case '\r':
                return next == '\n' ? 2 : 0;
            case '.':
                return next == ' ' ? 3 : 0;
            case ',':
                return next == ' ' ? 4 : 0;
            case ':':
                return next == ' ' ? 5 : 0;
            default:
                return 0;
        }
    }

    /** Adds the candidates that encode the single byte at {@code index}. */
    private List<AztecEncoderState> branchOverByte(List<AztecEncoderState> frontier, int index) {
        int value = input[index] & 0xFF;
        List<AztecEncoderState> candidates = new ArrayList<>();
        for (AztecEncoderState state : frontier) {
            int codeInCurrentMode = AztecModeTables.characterCode(state.mode(), value);
            boolean definedInCurrentMode = codeInCurrentMode != 0;

            // The open binary shift, if any, is closed only once the first non-binary candidate is
            // actually produced.
            AztecEncoderState withoutShift = null;
            for (AztecMode target : AztecMode.values()) {
                int code = AztecModeTables.characterCode(target, value);
                if (code == 0) {
                    continue;
                }
                if (withoutShift == null) {
                    withoutShift = state.closeShift(index);
                }
                if (!definedInCurrentMode || target == state.mode() || target == AztecMode.DIGIT) {
                    // Latching is worthwhile when the byte is absent from the current mode, when it
                    // stays in the same mode, or when DIGIT's cheaper codes make it pay off.
                    candidates.add(withoutShift.switchAndAppend(target, code));
                }
                if (!definedInCurrentMode && AztecModeTables.shiftCode(state.mode(), target) >= 0) {
                    // A shift into a mode that already holds the byte could never save bits.
                    candidates.add(withoutShift.appendWithShift(target, code));
                }
            }

            if (state.pendingShiftBytes() > 0 || !definedInCurrentMode) {
                candidates.add(state.addShiftedByte(index));
            }
        }
        return prune(candidates);
    }

    /** Adds the candidates that encode the two-character punctuation pair at {@code index}. */
    private List<AztecEncoderState> branchOverPair(List<AztecEncoderState> frontier, int index, int pairCode) {
        List<AztecEncoderState> candidates = new ArrayList<>();
        for (AztecEncoderState state : frontier) {
            AztecEncoderState withoutShift = state.closeShift(index);
            candidates.add(withoutShift.switchAndAppend(AztecMode.PUNCT, pairCode));
            if (state.mode() != AztecMode.PUNCT) {
                candidates.add(withoutShift.appendWithShift(AztecMode.PUNCT, pairCode));
            }
            if (pairCode == 3 || pairCode == 4) {
                // ". " and ", " are also expressible as two DIGIT codes.
                candidates.add(withoutShift
                        .switchAndAppend(AztecMode.DIGIT, 16 - pairCode)
                        .switchAndAppend(AztecMode.DIGIT, 1));
            }
            if (state.pendingShiftBytes() > 0) {
                candidates.add(state.addShiftedByte(index).addShiftedByte(index + 1));
            }
        }
        return prune(candidates);
    }

    /** Keeps only the candidates that are not dominated by another one. */
    private static List<AztecEncoderState> prune(List<AztecEncoderState> candidates) {
        Deque<AztecEncoderState> kept = new LinkedList<>();
        for (AztecEncoderState candidate : candidates) {
            boolean survives = true;
            for (Iterator<AztecEncoderState> iterator = kept.iterator(); iterator.hasNext(); ) {
                AztecEncoderState existing = iterator.next();
                if (existing.dominates(candidate)) {
                    survives = false;
                    break;
                }
                if (candidate.dominates(existing)) {
                    iterator.remove();
                }
            }
            if (survives) {
                kept.addFirst(candidate);
            }
        }
        return new ArrayList<>(kept);
    }
}
