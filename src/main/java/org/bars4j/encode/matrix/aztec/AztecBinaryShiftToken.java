package org.bars4j.encode.matrix.aztec;

/**
 * A token that emits a run of raw bytes in binary shift mode, including the binary shift header.
 */
final class AztecBinaryShiftToken extends AztecToken {

    private final int binaryShiftStart;
    private final int binaryShiftByteCount;

    AztecBinaryShiftToken(AztecToken previous, int binaryShiftStart, int binaryShiftByteCount) {
        super(previous);
        this.binaryShiftStart = binaryShiftStart;
        this.binaryShiftByteCount = binaryShiftByteCount;
    }

    @Override
    void appendTo(AztecBitBuffer buffer, byte[] text) {
        int byteCount = binaryShiftByteCount;
        for (int i = 0; i < byteCount; i++) {
            if (i == 0 || (i == 31 && byteCount <= 62)) {
                // A header opens the run; a second header is needed at byte 31 when the whole run
                // is at most 62 bytes, because the first header can only describe 31 of them.
                buffer.appendBits(31, 5); // BINARY_SHIFT
                if (byteCount > 62) {
                    buffer.appendBits(byteCount - 31, 16);
                } else if (i == 0) {
                    buffer.appendBits(Math.min(byteCount, 31), 5);
                } else {
                    buffer.appendBits(byteCount - 31, 5);
                }
            }
            buffer.appendBits(text[binaryShiftStart + i], 8);
        }
    }

    @Override
    public String toString() {
        return "<" + binaryShiftStart + "::" + (binaryShiftStart + binaryShiftByteCount - 1) + '>';
    }
}
