package org.bars4j.encode.matrix.qr;

import java.util.Arrays;

/**
 * Append-only stream of individual bits, used to assemble the QRCode data codewords.
 *
 * <p>Bits are stored most-significant-first in a growable byte array: the first appended bit
 * occupies bit 7 of {@code bytes[0]}, the ninth bit 7 of {@code bytes[1]}, and so on. Only the
 * first {@link #size()} bits are meaningful.
 */
public final class QrBitStream {

    private byte[] bytes = new byte[16];
    private int bitCount;

    /** Number of bits appended so far. */
    public int size() {
        return bitCount;
    }

    /** Number of whole bytes needed to hold the appended bits. */
    public int sizeInBytes() {
        return (bitCount + 7) / 8;
    }

    /** Returns the bit at {@code index}; indices outside the stream read as light ({@code false}). */
    public boolean bit(int index) {
        if (index < 0 || index >= bitCount) {
            return false;
        }
        return (bytes[index >>> 3] & (0x80 >>> (index & 7))) != 0;
    }

    /**
     * Appends the {@code width} least-significant bits of {@code value}, most significant bit
     * first. The bits of {@code value} above {@code width} must all be zero.
     */
    public void append(int value, int width) {
        if (width < 0 || width > 31) {
            throw new IllegalArgumentException("bit width out of range: " + width);
        }
        if (width < 31 && (value >>> width) != 0) {
            throw new IllegalArgumentException("value " + value + " does not fit in " + width + " bits");
        }
        grow(bitCount + width);
        for (int shift = width - 1; shift >= 0; shift--) {
            if (((value >>> shift) & 1) != 0) {
                bytes[bitCount >>> 3] |= (byte) (0x80 >>> (bitCount & 7));
            }
            bitCount++;
        }
    }

    /**
     * Grows the stream to exactly {@code capacityBytes} bytes, adding the four-bit terminator (if
     * it fits), zero bits up to the next byte boundary, then the alternating 0xEC / 0x11 pad
     * codewords until the capacity is reached.
     */
    public void padToCapacity(int capacityBytes) {
        int capacity = capacityBytes * 8;
        if (bitCount > capacity) {
            throw new IllegalArgumentException(
                    "bit stream already holds " + bitCount + " bits, capacity is " + capacity);
        }
        append(0, Math.min(4, capacity - bitCount));
        append(0, (-bitCount) & 7);
        boolean padWithEc = true;
        while (bitCount < capacity) {
            append(padWithEc ? 0xEC : 0x11, 8);
            padWithEc = !padWithEc;
        }
    }

    /** Returns the stream as codewords; the stream must end on a byte boundary. */
    public int[] toCodewords() {
        if ((bitCount & 7) != 0) {
            throw new IllegalStateException("bit stream is not byte aligned: " + bitCount + " bits");
        }
        int[] codewords = new int[bitCount >>> 3];
        for (int i = 0; i < codewords.length; i++) {
            codewords[i] = bytes[i] & 0xFF;
        }
        return codewords;
    }

    private void grow(int requiredBits) {
        int requiredBytes = (requiredBits + 7) / 8;
        if (requiredBytes > bytes.length) {
            bytes = Arrays.copyOf(bytes, Math.max(requiredBytes, bytes.length * 2));
        }
    }
}
