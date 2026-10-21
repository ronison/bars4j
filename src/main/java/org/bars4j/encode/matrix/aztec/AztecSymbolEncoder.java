package org.bars4j.encode.matrix.aztec;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.bars4j.encode.api.UnsupportedTextException;

/**
 * Core Aztec symbol generation: picks between compact and full layout and chooses the layer count,
 * stuffs and error-corrects the data bits, builds the mode message and lays the modules out in the
 * characteristic spiral around the central bull's-eye and (for full symbols) the reference grid.
 *
 * <p>Implements ISO/IEC 24778 without any external dependency.
 */
public final class AztecSymbolEncoder {

    /** Highest number of layers allowed for a full symbol. */
    private static final int MAX_LAYERS = 32;
    /** Highest number of layers allowed for a compact symbol. */
    private static final int MAX_COMPACT_LAYERS = 4;

    /** Codeword width (in bits) indexed by layer count. */
    private static final int[] WORD_SIZE = {
            4, 6, 6, 8, 8, 8, 8, 8, 8, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,
            12, 12, 12, 12, 12, 12, 12, 12, 12, 12,
    };

    private AztecSymbolEncoder() {
    }

    /**
     * Encodes {@code content} as an Aztec symbol.
     *
     * @param content     text to encode; without an explicit {@code charset} it must be representable
     *                    as ISO-8859-1 (Latin-1)
     * @param eccPercent  minimum percentage of error correction words
     * @param forcedLayers {@code null} or {@code 0} for automatic sizing; a negative value forces a
     *                    compact symbol with {@code abs(value)} layers, a positive value forces a
     *                    full symbol with that many layers
     * @param charset     character set to announce with an ECI, or {@code null} for no ECI
     */
    public static AztecCode encode(String content, int eccPercent, Integer forcedLayers, Charset charset)
            throws UnsupportedTextException {
        byte[] data = content.getBytes(charset != null ? charset : StandardCharsets.ISO_8859_1);
        AztecBitBuffer bits = new AztecHighLevelEncoder(data, charset).encode();

        int eccBits = bits.size() * eccPercent / 100 + 11;
        int totalSizeBits = bits.size() + eccBits;

        boolean compact;
        int layers;
        int totalBitsInLayer;
        int wordSize;
        AztecBitBuffer stuffedBits;
        if (forcedLayers != null && forcedLayers != 0) {
            compact = forcedLayers < 0;
            layers = Math.abs(forcedLayers);
            if (layers > (compact ? MAX_COMPACT_LAYERS : MAX_LAYERS)) {
                throw new IllegalArgumentException("Illegal value " + forcedLayers + " for layers");
            }
            totalBitsInLayer = totalBitsInLayer(layers, compact);
            wordSize = WORD_SIZE[layers];
            int usableBitsInLayers = totalBitsInLayer - (totalBitsInLayer % wordSize);
            stuffedBits = stuffBits(bits, wordSize);
            if (stuffedBits.size() + eccBits > usableBitsInLayers) {
                throw new UnsupportedTextException("Data too large for user specified layer");
            }
            if (compact && stuffedBits.size() > wordSize * 64) {
                throw new UnsupportedTextException("Data too large for user specified layer");
            }
        } else {
            wordSize = 0;
            stuffedBits = null;
            // Candidate sizes are tried in the order Compact1, Compact2, Compact3, Compact4,
            // Normal4, Normal5, ... Normal(i) for i < 4 is not used because Compact(i+1) has the
            // same size with more capacity.
            for (int i = 0; ; i++) {
                if (i > MAX_LAYERS) {
                    throw new UnsupportedTextException("Data too large for an Aztec code");
                }
                compact = i <= 3;
                layers = compact ? i + 1 : i;
                totalBitsInLayer = totalBitsInLayer(layers, compact);
                if (totalSizeBits > totalBitsInLayer) {
                    continue;
                }
                if (stuffedBits == null || wordSize != WORD_SIZE[layers]) {
                    wordSize = WORD_SIZE[layers];
                    stuffedBits = stuffBits(bits, wordSize);
                }
                int usableBitsInLayers = totalBitsInLayer - (totalBitsInLayer % wordSize);
                if (compact && stuffedBits.size() > wordSize * 64) {
                    // Compact symbols hold at most 64 data words.
                    continue;
                }
                if (stuffedBits.size() + eccBits <= usableBitsInLayers) {
                    break;
                }
            }
        }

        AztecBitBuffer messageBits = generateCheckWords(stuffedBits, totalBitsInLayer, wordSize);
        int messageSizeInWords = stuffedBits.size() / wordSize;
        AztecBitBuffer modeMessage = generateModeMessage(compact, layers, messageSizeInWords);

        int baseMatrixSize = (compact ? 11 : 14) + layers * 4; // excluding the reference grid
        int[] alignmentMap = new int[baseMatrixSize];
        int matrixSize;
        if (compact) {
            // Compact symbols have no reference grid, so the alignment map is the identity.
            matrixSize = baseMatrixSize;
            for (int i = 0; i < alignmentMap.length; i++) {
                alignmentMap[i] = i;
            }
        } else {
            matrixSize = baseMatrixSize + 1 + 2 * ((baseMatrixSize / 2 - 1) / 15);
            int origCenter = baseMatrixSize / 2;
            int center = matrixSize / 2;
            for (int i = 0; i < origCenter; i++) {
                int newOffset = i + i / 15;
                alignmentMap[origCenter - i - 1] = center - newOffset - 1;
                alignmentMap[origCenter + i] = center + newOffset + 1;
            }
        }
        AztecBitMatrix matrix = new AztecBitMatrix(matrixSize);

        // Draw the data bits, two columns and two rows per side of each layer.
        for (int i = 0, rowOffset = 0; i < layers; i++) {
            int rowSize = (layers - i) * 4 + (compact ? 9 : 12);
            for (int j = 0; j < rowSize; j++) {
                int columnOffset = j * 2;
                for (int k = 0; k < 2; k++) {
                    if (messageBits.get(rowOffset + columnOffset + k)) {
                        matrix.set(alignmentMap[i * 2 + k], alignmentMap[i * 2 + j]);
                    }
                    if (messageBits.get(rowOffset + rowSize * 2 + columnOffset + k)) {
                        matrix.set(alignmentMap[i * 2 + j], alignmentMap[baseMatrixSize - 1 - i * 2 - k]);
                    }
                    if (messageBits.get(rowOffset + rowSize * 4 + columnOffset + k)) {
                        matrix.set(alignmentMap[baseMatrixSize - 1 - i * 2 - k],
                                alignmentMap[baseMatrixSize - 1 - i * 2 - j]);
                    }
                    if (messageBits.get(rowOffset + rowSize * 6 + columnOffset + k)) {
                        matrix.set(alignmentMap[baseMatrixSize - 1 - i * 2 - j], alignmentMap[i * 2 + k]);
                    }
                }
            }
            rowOffset += rowSize * 8;
        }

        drawModeMessage(matrix, compact, matrixSize, modeMessage);

        if (compact) {
            drawBullsEye(matrix, matrixSize / 2, 5);
        } else {
            drawBullsEye(matrix, matrixSize / 2, 7);
            for (int i = 0, j = 0; i < baseMatrixSize / 2 - 1; i += 15, j += 16) {
                for (int k = (matrixSize / 2) & 1; k < matrixSize; k += 2) {
                    matrix.set(matrixSize / 2 - j, k);
                    matrix.set(matrixSize / 2 + j, k);
                    matrix.set(k, matrixSize / 2 - j);
                    matrix.set(k, matrixSize / 2 + j);
                }
            }
        }

        return new AztecCode(compact, layers, messageSizeInWords, matrix);
    }

    /** Draws the central bull's-eye of the given radius (in modules). */
    private static void drawBullsEye(AztecBitMatrix matrix, int center, int size) {
        for (int i = 0; i < size; i += 2) {
            for (int j = center - i; j <= center + i; j++) {
                matrix.set(j, center - i);
                matrix.set(j, center + i);
                matrix.set(center - i, j);
                matrix.set(center + i, j);
            }
        }
        matrix.set(center - size, center - size);
        matrix.set(center - size + 1, center - size);
        matrix.set(center - size, center - size + 1);
        matrix.set(center + size, center - size);
        matrix.set(center + size, center - size + 1);
        matrix.set(center + size, center + size - 1);
    }

    /** Builds the mode message (symbol geometry plus its Reed-Solomon check words). */
    static AztecBitBuffer generateModeMessage(boolean compact, int layers, int messageSizeInWords) {
        AztecBitBuffer modeMessage = new AztecBitBuffer();
        if (compact) {
            modeMessage.appendBits(layers - 1, 2);
            modeMessage.appendBits(messageSizeInWords - 1, 6);
            modeMessage = generateCheckWords(modeMessage, 28, 4);
        } else {
            modeMessage.appendBits(layers - 1, 5);
            modeMessage.appendBits(messageSizeInWords - 1, 11);
            modeMessage = generateCheckWords(modeMessage, 40, 4);
        }
        return modeMessage;
    }

    private static void drawModeMessage(AztecBitMatrix matrix, boolean compact, int matrixSize, AztecBitBuffer modeMessage) {
        int center = matrixSize / 2;
        if (compact) {
            for (int i = 0; i < 7; i++) {
                int offset = center - 3 + i;
                if (modeMessage.get(i)) {
                    matrix.set(offset, center - 5);
                }
                if (modeMessage.get(i + 7)) {
                    matrix.set(center + 5, offset);
                }
                if (modeMessage.get(20 - i)) {
                    matrix.set(offset, center + 5);
                }
                if (modeMessage.get(27 - i)) {
                    matrix.set(center - 5, offset);
                }
            }
        } else {
            for (int i = 0; i < 10; i++) {
                int offset = center - 5 + i + i / 5;
                if (modeMessage.get(i)) {
                    matrix.set(offset, center - 7);
                }
                if (modeMessage.get(i + 10)) {
                    matrix.set(center + 7, offset);
                }
                if (modeMessage.get(29 - i)) {
                    matrix.set(offset, center + 7);
                }
                if (modeMessage.get(39 - i)) {
                    matrix.set(center - 7, offset);
                }
            }
        }
    }

    /** Appends the Reed-Solomon check words of {@code wordSize} bits to {@code bitBuffer}. */
    private static AztecBitBuffer generateCheckWords(AztecBitBuffer bitBuffer, int totalBits, int wordSize) {
        // bitBuffer is always a multiple of wordSize, so no padding is required.
        int messageSizeInWords = bitBuffer.size() / wordSize;
        GenericReedSolomonEncoder rs = new GenericReedSolomonEncoder(fieldForWordSize(wordSize));
        int totalWords = totalBits / wordSize;
        int[] messageWords = bitsToWords(bitBuffer, wordSize, totalWords);
        rs.encode(messageWords, totalWords - messageSizeInWords);
        int startPad = totalBits % wordSize;
        AztecBitBuffer messageBits = new AztecBitBuffer();
        messageBits.appendBits(0, startPad);
        for (int messageWord : messageWords) {
            messageBits.appendBits(messageWord, wordSize);
        }
        return messageBits;
    }

    /** Packs the leading words of {@code bits} into an array of {@code totalWords} codewords. */
    private static int[] bitsToWords(AztecBitBuffer bits, int wordSize, int totalWords) {
        int[] message = new int[totalWords];
        int words = bits.size() / wordSize;
        for (int i = 0; i < words; i++) {
            int value = 0;
            for (int j = 0; j < wordSize; j++) {
                value |= bits.get(i * wordSize + j) ? (1 << (wordSize - j - 1)) : 0;
            }
            message[i] = value;
        }
        return message;
    }

    private static GenericGaloisField fieldForWordSize(int wordSize) {
        switch (wordSize) {
            case 4:
                return GenericGaloisField.AZTEC_PARAM;
            case 6:
                return GenericGaloisField.AZTEC_DATA_6;
            case 8:
                return GenericGaloisField.AZTEC_DATA_8;
            case 10:
                return GenericGaloisField.AZTEC_DATA_10;
            case 12:
                return GenericGaloisField.AZTEC_DATA_12;
            default:
                throw new IllegalArgumentException("Unsupported word size " + wordSize);
        }
    }

    /**
     * Splits {@code bits} into {@code wordSize}-bit words, inserting stuffing when a word would
     * otherwise read as all zeros or all ones (which are reserved for the decoder's unstuffing).
     */
    static AztecBitBuffer stuffBits(AztecBitBuffer bits, int wordSize) {
        AztecBitBuffer out = new AztecBitBuffer();
        int n = bits.size();
        int mask = (1 << wordSize) - 2;
        for (int i = 0; i < n; i += wordSize) {
            int word = 0;
            for (int j = 0; j < wordSize; j++) {
                if (i + j >= n || bits.get(i + j)) {
                    word |= 1 << (wordSize - 1 - j);
                }
            }
            if ((word & mask) == mask) {
                out.appendBits(word & mask, wordSize);
                i--;
            } else if ((word & mask) == 0) {
                out.appendBits(word | 1, wordSize);
                i--;
            } else {
                out.appendBits(word, wordSize);
            }
        }
        return out;
    }

    /** Returns the number of data bits in {@code layers} layers of the given layout. */
    private static int totalBitsInLayer(int layers, boolean compact) {
        return ((compact ? 88 : 112) + 16 * layers) * layers;
    }
}
