package org.bars4j.encode.matrix.qr;

import java.util.BitSet;

/**
 * Growable bit buffer used to assemble the QRCode data bit stream.
 */
public final class QrBitStream {

    private final BitSet bits = new BitSet();
    private int size;

    public int size() {
        return size;
    }

    public int sizeInBytes() {
        return (size + 7) / 8;
    }

    public boolean bit(int index) {
        return bits.get(index);
    }

    /** Appends the low {@code numBits} of {@code value}, most significant bit first. */
    public void append(int value, int numBits) {
        if (numBits < 0 || numBits > 31) {
            throw new IllegalArgumentException("numBits out of range: " + numBits);
        }
        if (numBits < 31 && (value >>> numBits) != 0) {
            throw new IllegalArgumentException("value " + value + " does not fit in " + numBits + " bits");
        }
        for (int i = numBits - 1; i >= 0; i--) {
            if (((value >>> i) & 1) != 0) {
                bits.set(size);
            }
            size++;
        }
    }

    /**
     * Completes the stream up to {@code capacityBytes}: terminator, byte alignment and the
     * alternating 0xEC / 0x11 pad codewords.
     */
    public void padToCapacity(int capacityBytes) {
        int capacityBits = capacityBytes * 8;
        int terminator = Math.min(4, capacityBits - size);
        append(0, terminator);
        while (size % 8 != 0) {
            append(0, 1);
        }
        boolean useEc = true;
        while (size < capacityBits) {
            append(useEc ? 0xEC : 0x11, 8);
            useEc = !useEc;
        }
    }

    /** Converts the bit stream to codewords (assumes the stream is byte aligned). */
    public int[] toCodewords() {
        int[] codewords = new int[sizeInBytes()];
        for (int i = 0; i < size; i++) {
            if (bits.get(i)) {
                codewords[i / 8] |= 0x80 >>> (i % 8);
            }
        }
        return codewords;
    }
}
