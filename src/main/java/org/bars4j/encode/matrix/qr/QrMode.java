package org.bars4j.encode.matrix.qr;

/**
 * QRCode data encoding modes.
 *
 * <p>{@code charCountBits} holds the width of the character-count indicator per version group:
 * index 0 = versions 1-9, 1 = versions 10-26, 2 = versions 27-40.
 */
public enum QrMode {
    NUMERIC(0x1, new int[]{10, 12, 14}),
    ALPHANUMERIC(0x2, new int[]{9, 11, 13}),
    BYTE(0x4, new int[]{8, 16, 16});

    private final int indicatorBits;
    private final int[] charCountBits;

    QrMode(int indicatorBits, int[] charCountBits) {
        this.indicatorBits = indicatorBits;
        this.charCountBits = charCountBits;
    }

    public int indicatorBits() {
        return indicatorBits;
    }

    /** Width of the character-count indicator for the given version (1-40). */
    public int charCountBitsForVersion(int version) {
        if (version <= 9) {
            return charCountBits[0];
        }
        if (version <= 26) {
            return charCountBits[1];
        }
        return charCountBits[2];
    }
}
