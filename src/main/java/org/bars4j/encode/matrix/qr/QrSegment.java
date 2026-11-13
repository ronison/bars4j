package org.bars4j.encode.matrix.qr;

import java.util.Arrays;

/**
 * A run of data encoded in a single {@link QrMode}.
 *
 * @param values          the packed values (digit groups, alphanumeric pairs or raw bytes)
 * @param bitWidths       bit width of each value in {@code values}
 * @param charCount       number of input characters represented by this segment
 */
public record QrSegment(QrMode mode, int[] values, int[] bitWidths, int charCount) {

    /** Number of bits used by the data portion (excluding mode and character count indicators). */
    public int dataBitLength() {
        int total = 0;
        for (int width : bitWidths) {
            total += width;
        }
        return total;
    }

    /** Writes mode indicator, character count and data into the given stream. */
    public void appendTo(QrBitStream stream, int version) {
        stream.append(mode.indicatorBits(), 4);
        stream.append(charCount, mode.charCountBitsForVersion(version));
        for (int i = 0; i < values.length; i++) {
            stream.append(values[i], bitWidths[i]);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof QrSegment other
                && mode == other.mode
                && Arrays.equals(values, other.values)
                && Arrays.equals(bitWidths, other.bitWidths)
                && charCount == other.charCount;
    }

    @Override
    public int hashCode() {
        int result = mode != null ? mode.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(values);
        result = 31 * result + Arrays.hashCode(bitWidths);
        result = 31 * result + charCount;
        return result;
    }

    @Override
    public String toString() {
        return "QrSegment[mode=" + mode
                + ", values=" + Arrays.toString(values)
                + ", bitWidths=" + Arrays.toString(bitWidths)
                + ", charCount=" + charCount + "]";
    }
}
