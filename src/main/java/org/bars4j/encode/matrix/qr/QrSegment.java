package org.bars4j.encode.matrix.qr;

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
}
