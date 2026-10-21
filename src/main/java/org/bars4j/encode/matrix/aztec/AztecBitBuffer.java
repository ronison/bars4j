package org.bars4j.encode.matrix.aztec;

import java.util.Arrays;

/**
 * Growable buffer of bits used while assembling the Aztec bit stream. Bits are appended most
 * significant bit first, so that a word written with {@link #appendBits} reads back in the natural
 * left-to-right order.
 */
public final class AztecBitBuffer {

    private byte[] bytes = new byte[32];
    private int bitCount;

    /** Returns the number of bits written so far. */
    public int size() {
        return bitCount;
    }

    /** Returns bit {@code index} ({@code true} = one). */
    public boolean get(int index) {
        if (index < 0 || index >= bitCount) {
            throw new IndexOutOfBoundsException("bit index " + index + " out of range [0," + bitCount + ")");
        }
        return (bytes[index / 8] & (1 << (7 - index % 8))) != 0;
    }

    /** Appends a single bit. */
    public void appendBit(boolean bit) {
        ensureCapacity(1);
        if (bit) {
            bytes[bitCount / 8] |= (byte) (1 << (7 - bitCount % 8));
        }
        bitCount++;
    }

    /**
     * Appends the low {@code numBits} of {@code value}, most significant bit first.
     */
    public void appendBits(int value, int numBits) {
        if (numBits < 0 || numBits > 32) {
            throw new IllegalArgumentException("numBits out of range: " + numBits);
        }
        ensureCapacity(numBits);
        for (int i = numBits - 1; i >= 0; i--) {
            appendBit(((value >>> i) & 1) != 0);
        }
    }

    private void ensureCapacity(int additionalBits) {
        int neededBytes = (bitCount + additionalBits + 7) / 8;
        if (neededBytes > bytes.length) {
            bytes = Arrays.copyOf(bytes, Math.max(neededBytes, bytes.length * 2));
        }
    }
}
