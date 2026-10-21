package org.bars4j.encode.matrix.aztec;

/**
 * A single step of the Aztec high-level encoding, stored as a backwards-linked list. Each token
 * knows how to append its own bits to the output buffer.
 */
abstract class AztecToken {

    /** The empty token that terminates the list. */
    static final AztecToken EMPTY = new AztecSimpleToken(null, 0, 0);

    private final AztecToken previous;

    AztecToken(AztecToken previous) {
        this.previous = previous;
    }

    /** Returns the token that precedes this one, or {@code null} for {@link #EMPTY}. */
    final AztecToken previous() {
        return previous;
    }

    /** Returns a new token that appends {@code bitCount} bits of {@code value} after this one. */
    final AztecToken add(int value, int bitCount) {
        return new AztecSimpleToken(this, value, bitCount);
    }

    /** Returns a new token that appends a binary shift run after this one. */
    final AztecToken addBinaryShift(int start, int byteCount) {
        return new AztecBinaryShiftToken(this, start, byteCount);
    }

    /** Appends this token's bits to {@code buffer}, reading any binary data from {@code text}. */
    abstract void appendTo(AztecBitBuffer buffer, byte[] text);
}
