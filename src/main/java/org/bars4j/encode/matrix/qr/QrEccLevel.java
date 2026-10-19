package org.bars4j.encode.matrix.qr;

/** QRCode error correction level. The {@code formatBits} are the 2-bit values stored in format info. */
public enum QrEccLevel {
    /** ~7% recovery. */
    L(0b01),
    /** ~15% recovery. */
    M(0b00),
    /** ~25% recovery. */
    Q(0b11),
    /** ~30% recovery. */
    H(0b10);

    private final int formatBits;

    QrEccLevel(int formatBits) {
        this.formatBits = formatBits;
    }

    public int formatBits() {
        return formatBits;
    }
}
