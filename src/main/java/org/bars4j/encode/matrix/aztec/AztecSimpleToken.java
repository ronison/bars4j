package org.bars4j.encode.matrix.aztec;

/**
 * A token that emits a fixed bit pattern (a latch, a shift or a mode code).
 */
final class AztecSimpleToken extends AztecToken {

    private final int value;
    private final int bitCount;

    AztecSimpleToken(AztecToken previous, int value, int bitCount) {
        super(previous);
        this.value = value;
        this.bitCount = bitCount;
    }

    @Override
    void appendTo(AztecBitBuffer buffer, byte[] text) {
        buffer.appendBits(value, bitCount);
    }

    @Override
    public String toString() {
        return "<" + Integer.toBinaryString(value | (1 << bitCount)).substring(1) + '>';
    }
}
